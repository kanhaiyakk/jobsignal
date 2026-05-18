package com.jobsignal.enricher.messaging.producer;

import com.jobsignal.shared.event.EnrichedListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichedListingKafkaProducer implements EnrichedListingProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.enriched-listings}")
    private String topic;

    @Override
    public void publish(EnrichedListingEvent event) {
        kafkaTemplate.send(topic, event.payload().externalId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish enriched listing externalId={}",
                                event.payload().externalId(), ex);
                    } else {
                        log.debug("Published enriched listing externalId={}",
                                event.payload().externalId());
                    }
                });
    }
}
