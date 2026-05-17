package com.jobsignal.normalizer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobsignal.shared.event.NormalizedListingEvent;
import com.jobsignal.shared.event.RawListingEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RawListingConsumerIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void whenRawListingPublished_normalizedListingAppearsOnOutputTopic() throws Exception {
        String externalId = "ext-it-1";
        RawListingEvent event = buildRawEvent(externalId, "remoteok");

        kafkaTemplate.send("raw-listings", event.payload().externalId(), event).get();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(), "it-consumer-group", "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(java.util.List.of("normalized-listings"));
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);

            NormalizedListingEvent matched = null;
            for (var record : records) {
                NormalizedListingEvent normalized = objectMapper.readValue(record.value(), NormalizedListingEvent.class);
                if (externalId.equals(normalized.payload().externalId())) {
                    matched = normalized;
                    break;
                }
            }

            assertThat(matched).as("expected record with externalId=%s on normalized-listings", externalId).isNotNull();
            assertThat(matched.payload().source()).isEqualTo("remoteok");
            assertThat(matched.eventType()).isEqualTo("LISTING_NORMALIZED");
        }
    }

    @Test
    void whenRawListingWithHtmlDescription_descriptionIsStrippedInOutput() throws Exception {
        RawListingEvent event = buildRawEventWithDescription("ext-it-2",
                "<p>We need a <strong>Java</strong> engineer.</p>");

        kafkaTemplate.send("raw-listings", event.payload().externalId(), event).get();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(), "it-consumer-html-group", "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(java.util.List.of("normalized-listings"));
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15));

            assertThat(records.count()).isGreaterThanOrEqualTo(1);

            // Find the record matching our externalId
            for (var record : records) {
                NormalizedListingEvent normalized = objectMapper.readValue(record.value(),
                        NormalizedListingEvent.class);
                if ("ext-it-2".equals(normalized.payload().externalId())) {
                    assertThat(normalized.payload().descriptionText())
                            .isEqualTo("We need a Java engineer.");
                    return;
                }
            }
        }
    }

    private RawListingEvent buildRawEvent(String externalId, String source) {
        return buildRawEventWithDescription(externalId, "Plain text description.");
    }

    private RawListingEvent buildRawEventWithDescription(String externalId, String description) {
        return new RawListingEvent(
                UUID.randomUUID(), "RAW_LISTING_SCRAPED", Instant.now(), 1,
                new RawListingEvent.RawListingPayload(
                        UUID.randomUUID().toString(), "remoteok", externalId,
                        "Senior Java Engineer", "Acme Corp", "Remote",
                        description, "https://apply.co", Instant.now(), "{}"
                )
        );
    }
}
