package com.jobsignal.scraper.messaging.producer;

import com.jobsignal.shared.event.RawListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawListingKafkaProducer implements RawListingProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.raw-listings}")
    private String topic;

    @Override
    public void publish(RawListingEvent event) {
        kafkaTemplate.send(topic, event.payload().externalId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish raw listing event id={} externalId={}: {}",
                                event.eventId(), event.payload().externalId(), ex.getMessage());
                    } else {
                        log.debug("Published raw listing event id={} externalId={} to topic={}",
                                event.eventId(), event.payload().externalId(), topic);
                    }
                });
    }
}
