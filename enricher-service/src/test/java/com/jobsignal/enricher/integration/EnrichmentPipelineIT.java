package com.jobsignal.enricher.integration;

import com.jobsignal.enricher.client.GeminiClient;
import com.jobsignal.enricher.model.ExtractionResult;
import com.jobsignal.enricher.persistence.repository.EnrichedListingRepository;
import com.jobsignal.shared.event.NormalizedListingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
class EnrichmentPipelineIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16"))
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @MockBean
    private GeminiClient geminiClient;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EnrichedListingRepository repository;

    private static final float[] FAKE_EMBEDDING = new float[768];

    static {
        for (int i = 0; i < FAKE_EMBEDDING.length; i++) FAKE_EMBEDDING[i] = (float) i / 768;
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(geminiClient.embed(anyString())).thenReturn(FAKE_EMBEDDING);
        when(geminiClient.extract(anyString())).thenReturn(
                new ExtractionResult(List.of("Java", "Spring Boot"), "SENIOR", "not specified", 5, "REMOTE")
        );
    }

    @Test
    void whenNormalizedListingPublished_enrichedListingIsPersistedToDatabase() throws Exception {
        String externalId = "ext-enrich-it-1";
        NormalizedListingEvent event = buildEvent("remoteok", externalId);

        kafkaTemplate.send("normalized-listings", externalId, event).get();

        await().atMost(Duration.ofSeconds(20))
                .until(() -> repository.existsBySourceAndExternalId("remoteok", externalId));

        var saved = repository.findAll().stream()
                .filter(e -> externalId.equals(e.getExternalId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Entity not found"));

        assertThat(saved.getSeniority()).isEqualTo("SENIOR");
        assertThat(saved.getRemotePolicy()).isEqualTo("REMOTE");
        assertThat(saved.getTechStack()).contains("Java", "Spring Boot");
        assertThat(saved.getEmbedding()).hasSize(768);
    }

    @Test
    void whenDuplicatePublished_onlyOneRecordIsPersisted() throws Exception {
        String externalId = "ext-enrich-it-dup";
        NormalizedListingEvent event = buildEvent("remoteok", externalId);

        kafkaTemplate.send("normalized-listings", externalId, event).get();
        kafkaTemplate.send("normalized-listings", externalId, event).get();

        await().atMost(Duration.ofSeconds(20))
                .until(() -> repository.existsBySourceAndExternalId("remoteok", externalId));

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> true);

        long count = repository.findAll().stream()
                .filter(e -> externalId.equals(e.getExternalId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    private NormalizedListingEvent buildEvent(String source, String externalId) {
        return new NormalizedListingEvent(
                UUID.randomUUID(), "LISTING_NORMALIZED", Instant.now(), 1,
                new NormalizedListingEvent.NormalizedListingPayload(
                        UUID.randomUUID().toString(),
                        source, externalId,
                        "Senior Java Engineer", "Acme Corp", "Remote",
                        "We are hiring a Java engineer with Spring Boot and PostgreSQL experience.",
                        "https://apply.co", Instant.now(), true
                )
        );
    }
}
