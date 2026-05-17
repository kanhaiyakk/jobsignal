package com.jobsignal.normalizer.messaging.producer;

import com.jobsignal.shared.event.NormalizedListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NormalizedListingKafkaProducer implements NormalizedListingProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.normalized-listings}")
    private String topic;

    @Override
    public void publish(NormalizedListingEvent event) {
        kafkaTemplate.send(topic, event.payload().externalId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish normalized listing event id={} externalId={}: {}",
                                event.eventId(), event.payload().externalId(), ex.getMessage());
                    } else {
                        log.debug("Published normalized listing event id={} externalId={} to topic={}",
                                event.eventId(), event.payload().externalId(), topic);
                    }
                });
    }
}
