package com.jobsignal.insights.integration;

import com.jobsignal.insights.persistence.repository.ListingSnapshotRepository;
import com.jobsignal.shared.event.EnrichedListingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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

@SpringBootTest
@Testcontainers
class InsightPipelineIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ListingSnapshotRepository snapshotRepository;

    @BeforeEach
    void setUp() {
        snapshotRepository.deleteAll();
    }

    @Test
    void whenEnrichedListingPublished_snapshotIsPersistedToDatabase() throws Exception {
        String externalId = "it-ext-1";
        EnrichedListingEvent event = buildEvent("remoteok", externalId);

        kafkaTemplate.send("enriched-listings", externalId, event).get();

        await().atMost(Duration.ofSeconds(20))
                .until(() -> snapshotRepository.existsBySourceAndExternalId("remoteok", externalId));

        var saved = snapshotRepository.findAll().stream()
                .filter(e -> externalId.equals(e.getExternalId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Snapshot not found"));

        assertThat(saved.getSeniority()).isEqualTo("SENIOR");
        assertThat(saved.getRemotePolicy()).isEqualTo("REMOTE");
        assertThat(saved.getTechStack()).contains("Java", "Spring Boot");
    }

    @Test
    void whenDuplicateEnrichedListing_onlyOneSnapshotPersisted() throws Exception {
        String externalId = "it-ext-dup";
        EnrichedListingEvent event = buildEvent("remoteok", externalId);

        kafkaTemplate.send("enriched-listings", externalId, event).get();
        kafkaTemplate.send("enriched-listings", externalId, event).get();

        await().atMost(Duration.ofSeconds(20))
                .until(() -> snapshotRepository.existsBySourceAndExternalId("remoteok", externalId));

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> true);

        long count = snapshotRepository.findAll().stream()
                .filter(e -> externalId.equals(e.getExternalId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    private EnrichedListingEvent buildEvent(String source, String externalId) {
        return new EnrichedListingEvent(
                UUID.randomUUID(), "LISTING_ENRICHED", Instant.now(), 1,
                new EnrichedListingEvent.EnrichedListingPayload(
                        UUID.randomUUID().toString(),
                        source, externalId,
                        "Senior Java Engineer", "Acme Corp", "Remote",
                        "We are looking for a Java engineer with Spring Boot skills.",
                        "https://apply.co", Instant.now(), true,
                        List.of("Java", "Spring Boot"), "SENIOR", "$120k", 5, "REMOTE"
                )
        );
    }
}
