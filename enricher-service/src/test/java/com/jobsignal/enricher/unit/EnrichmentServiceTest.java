package com.jobsignal.enricher.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobsignal.enricher.client.GeminiClient;
import com.jobsignal.enricher.config.EnricherProperties;
import com.jobsignal.enricher.messaging.producer.EnrichedListingProducer;
import com.jobsignal.enricher.model.ExtractionResult;
import com.jobsignal.enricher.persistence.entity.EnrichedListingEntity;
import com.jobsignal.enricher.persistence.repository.EnrichedListingRepository;
import com.jobsignal.enricher.service.EnrichmentService;
import com.jobsignal.shared.event.NormalizedListingEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrichmentServiceTest {

    @Mock private GeminiClient geminiClient;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private EnrichedListingRepository repository;
    @Mock private EnrichedListingProducer producer;

    private EnrichmentService service;
    private final float[] fakeEmbedding = new float[]{0.1f, 0.2f, 0.3f};
    private final ExtractionResult fakeExtraction = new ExtractionResult(
            List.of("Java", "Spring Boot"), "SENIOR", "not specified", 5, "REMOTE");

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        EnricherProperties props = new EnricherProperties(0.92, 604800L, 604800L);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        service = new EnrichmentService(
                geminiClient,
                redisTemplate,
                repository,
                producer,
                props,
                new SimpleMeterRegistry(),
                objectMapper,
                new ClassPathResource("prompts/extraction-v1.txt")
        );
    }

    @Test
    void enrich_whenNewListing_callsGeminiAndPersists() {
        NormalizedListingEvent event = buildEvent("remoteok", "ext-1");
        when(repository.existsBySourceAndExternalId(anyString(), anyString())).thenReturn(false);
        when(repository.existsNearDuplicate(anyString(), anyDouble())).thenReturn(false);
        when(geminiClient.embed(anyString())).thenReturn(fakeEmbedding);
        when(geminiClient.extract(anyString())).thenReturn(fakeExtraction);

        service.enrich(event);

        verify(geminiClient, times(1)).embed(anyString());
        verify(geminiClient, times(1)).extract(anyString());
        ArgumentCaptor<EnrichedListingEntity> captor = ArgumentCaptor.forClass(EnrichedListingEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSeniority()).isEqualTo("SENIOR");
        assertThat(captor.getValue().getRemotePolicy()).isEqualTo("REMOTE");
        verify(producer).publish(any());
    }

    @Test
    void enrich_whenExactDuplicate_skipsImmediately() {
        NormalizedListingEvent event = buildEvent("remoteok", "ext-dup");
        when(repository.existsBySourceAndExternalId("remoteok", "ext-dup")).thenReturn(true);

        service.enrich(event);

        verify(geminiClient, never()).embed(anyString());
        verify(repository, never()).save(any());
        verify(producer, never()).publish(any());
    }

    @Test
    void enrich_whenNearDuplicate_skipsWithoutPersisting() {
        NormalizedListingEvent event = buildEvent("remoteok", "ext-near-dup");
        when(repository.existsBySourceAndExternalId(anyString(), anyString())).thenReturn(false);
        when(repository.existsNearDuplicate(anyString(), anyDouble())).thenReturn(true);
        when(geminiClient.embed(anyString())).thenReturn(fakeEmbedding);

        service.enrich(event);

        verify(geminiClient, times(1)).embed(anyString());
        verify(geminiClient, never()).extract(anyString());
        verify(repository, never()).save(any());
        verify(producer, never()).publish(any());
    }

    @Test
    void enrich_whenEmbeddingCached_skipsGeminiEmbedCall() {
        NormalizedListingEvent event = buildEvent("remoteok", "ext-cached");
        when(repository.existsBySourceAndExternalId(anyString(), anyString())).thenReturn(false);
        when(repository.existsNearDuplicate(anyString(), anyDouble())).thenReturn(false);
        when(valueOps.get(anyString()))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0);
                    if (key.startsWith("cache:gemini:embed:")) return "[0.1,0.2,0.3]";
                    return null;
                });
        when(geminiClient.extract(anyString())).thenReturn(fakeExtraction);

        service.enrich(event);

        verify(geminiClient, never()).embed(anyString());
        verify(geminiClient, times(1)).extract(anyString());
        verify(repository).save(any());
    }

    @Test
    void enrich_whenExtractionCached_skipsGeminiExtractCall() throws Exception {
        NormalizedListingEvent event = buildEvent("remoteok", "ext-extract-cached");
        when(repository.existsBySourceAndExternalId(anyString(), anyString())).thenReturn(false);
        when(repository.existsNearDuplicate(anyString(), anyDouble())).thenReturn(false);
        when(geminiClient.embed(anyString())).thenReturn(fakeEmbedding);
        String cachedJson = new ObjectMapper().registerModule(new JavaTimeModule())
                .writeValueAsString(fakeExtraction);
        when(valueOps.get(anyString()))
                .thenAnswer(inv -> {
                    String key = inv.getArgument(0);
                    if (key.startsWith("cache:gemini:extract:")) return cachedJson;
                    return null;
                });

        service.enrich(event);

        verify(geminiClient, never()).extract(anyString());
        verify(repository).save(any());
    }

    private NormalizedListingEvent buildEvent(String source, String externalId) {
        return new NormalizedListingEvent(
                UUID.randomUUID(), "LISTING_NORMALIZED", Instant.now(), 1,
                new NormalizedListingEvent.NormalizedListingPayload(
                        UUID.randomUUID().toString(),
                        source, externalId,
                        "Senior Java Engineer", "Acme Corp", "Remote",
                        "We are hiring a Java engineer with Spring Boot experience.",
                        "https://apply.co", Instant.now(), true
                )
        );
    }
}
