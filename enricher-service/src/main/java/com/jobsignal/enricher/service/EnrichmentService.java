package com.jobsignal.enricher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsignal.enricher.client.GeminiClient;
import com.jobsignal.enricher.config.EnricherProperties;
import com.jobsignal.enricher.messaging.producer.EnrichedListingProducer;
import com.jobsignal.enricher.model.ExtractionResult;
import com.jobsignal.enricher.persistence.converter.FloatArrayVectorType;
import com.jobsignal.enricher.persistence.entity.EnrichedListingEntity;
import com.jobsignal.enricher.persistence.repository.EnrichedListingRepository;
import com.jobsignal.shared.event.EnrichedListingEvent;
import com.jobsignal.shared.event.NormalizedListingEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Slf4j
public class EnrichmentService {

    private final GeminiClient geminiClient;
    private final StringRedisTemplate redisTemplate;
    private final EnrichedListingRepository repository;
    private final EnrichedListingProducer producer;
    private final EnricherProperties properties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;

    public EnrichmentService(
            GeminiClient geminiClient,
            StringRedisTemplate redisTemplate,
            EnrichedListingRepository repository,
            EnrichedListingProducer producer,
            EnricherProperties properties,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper,
            @Value("classpath:prompts/extraction-v1.txt") Resource extractionPromptResource
    ) throws IOException {
        this.geminiClient = geminiClient;
        this.redisTemplate = redisTemplate;
        this.repository = repository;
        this.producer = producer;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.promptTemplate = extractionPromptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Transactional
    public void enrich(NormalizedListingEvent event) {
        var payload = event.payload();
        Timer.Sample sample = Timer.start(meterRegistry);

        if (repository.existsBySourceAndExternalId(payload.source(), payload.externalId())) {
            log.debug("Exact duplicate skipped source={} externalId={}", payload.source(), payload.externalId());
            return;
        }

        String contentHash = computeHash(payload.title(), payload.company(), payload.descriptionText());

        float[] embedding = resolveEmbedding(contentHash, payload);

        double maxDistance = 1.0 - properties.deduplicationThreshold();
        if (repository.existsNearDuplicate(serializeVector(embedding), maxDistance)) {
            meterRegistry.counter("listings.deduped").increment();
            log.info("Near-duplicate skipped source={} externalId={}", payload.source(), payload.externalId());
            return;
        }

        ExtractionResult extraction = resolveExtraction(contentHash, payload);

        EnrichedListingEntity entity = EnrichedListingEntity.from(payload, extraction, embedding, contentHash);
        repository.save(entity);

        producer.publish(toEvent(event, entity, extraction));

        sample.stop(Timer.builder("enrichment.duration").register(meterRegistry));
        log.info("Enriched listing source={} externalId={}", payload.source(), payload.externalId());
    }

    private float[] resolveEmbedding(String contentHash,
                                     NormalizedListingEvent.NormalizedListingPayload payload) {
        String cacheKey = "cache:gemini:embed:" + contentHash;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserializeVector(cached);
        }
        String text = payload.title() + " " + payload.company() + " " + payload.descriptionText();
        float[] embedding = geminiClient.embed(text);
        redisTemplate.opsForValue().set(cacheKey, serializeVector(embedding),
                Duration.ofSeconds(properties.embeddingCacheTtlSeconds()));
        return embedding;
    }

    private ExtractionResult resolveExtraction(String contentHash,
                                               NormalizedListingEvent.NormalizedListingPayload payload) {
        String cacheKey = "cache:gemini:extract:" + contentHash;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, ExtractionResult.class);
            } catch (JsonProcessingException e) {
                log.warn("Stale extraction cache entry, re-extracting contentHash={}", contentHash);
            }
        }
        String prompt = buildPrompt(payload);
        ExtractionResult result = geminiClient.extract(prompt);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(properties.extractionCacheTtlSeconds()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache extraction result contentHash={}", contentHash);
        }
        return result;
    }

    private String buildPrompt(NormalizedListingEvent.NormalizedListingPayload payload) {
        return promptTemplate
                .replace("{title}", payload.title())
                .replace("{company}", payload.company())
                .replace("{location}", payload.location())
                .replace("{description}", payload.descriptionText());
    }

    private String computeHash(String title, String company, String description) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = (title + "|" + company + "|" + description).trim();
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String serializeVector(float[] vector) {
        return FloatArrayVectorType.serialize(vector);
    }

    private float[] deserializeVector(String raw) {
        String inner = raw.substring(1, raw.length() - 1);
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private EnrichedListingEvent toEvent(NormalizedListingEvent source,
                                         EnrichedListingEntity entity,
                                         ExtractionResult extraction) {
        return new EnrichedListingEvent(
                UUID.randomUUID(),
                "LISTING_ENRICHED",
                Instant.now(),
                1,
                new EnrichedListingEvent.EnrichedListingPayload(
                        entity.getNormalizedListingId(),
                        entity.getSource(),
                        entity.getExternalId(),
                        entity.getTitle(),
                        entity.getCompany(),
                        entity.getLocation(),
                        entity.getDescriptionText(),
                        entity.getApplyUrl(),
                        entity.getPostedAt(),
                        entity.isRemote(),
                        extraction.techStack(),
                        extraction.seniority(),
                        extraction.salaryRange(),
                        extraction.experienceYears(),
                        extraction.remotePolicy()
                )
        );
    }
}
