package com.jobsignal.storage.integration;

import com.jobsignal.shared.event.NormalizedListingEvent;
import com.jobsignal.storage.persistence.repository.NormalizedListingRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class NormalizedListingConsumerIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NormalizedListingRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void whenNormalizedListingPublished_itIsPersistedToDatabase() throws Exception {
        String externalId = "ext-storage-it-1";
        NormalizedListingEvent event = buildEvent("remoteok", externalId, true);

        kafkaTemplate.send("normalized-listings", externalId, event).get();

        await().atMost(Duration.ofSeconds(15))
                .until(() -> repository.existsBySourceAndExternalId("remoteok", externalId));

        assertThat(repository.existsBySourceAndExternalId("remoteok", externalId)).isTrue();
    }

    @Test
    void whenDuplicateNormalizedListingPublished_onlyOneRecordIsPersisted() throws Exception {
        String externalId = "ext-storage-it-dup";
        NormalizedListingEvent event = buildEvent("remoteok", externalId, false);

        kafkaTemplate.send("normalized-listings", externalId, event).get();
        kafkaTemplate.send("normalized-listings", externalId, event).get();

        await().atMost(Duration.ofSeconds(15))
                .until(() -> repository.existsBySourceAndExternalId("remoteok", externalId));

        // Brief wait to allow any in-flight duplicate message to be processed
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> true);

        long count = repository.findAll().stream()
                .filter(e -> "remoteok".equals(e.getSource()) && externalId.equals(e.getExternalId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    private NormalizedListingEvent buildEvent(String source, String externalId, boolean isRemote) {
        return new NormalizedListingEvent(
                UUID.randomUUID(), "LISTING_NORMALIZED", Instant.now(), 1,
                new NormalizedListingEvent.NormalizedListingPayload(
                        UUID.randomUUID().toString(),
                        source, externalId,
                        "Senior Java Engineer", "Acme Corp", "Remote",
                        "We are hiring a Java engineer.",
                        "https://apply.co",
                        Instant.now(),
                        isRemote
                )
        );
    }
}
