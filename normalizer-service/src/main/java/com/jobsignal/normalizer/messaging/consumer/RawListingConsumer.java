package com.jobsignal.normalizer.messaging.consumer;

import com.jobsignal.normalizer.messaging.producer.NormalizedListingProducer;
import com.jobsignal.normalizer.service.NormalizationService;
import com.jobsignal.shared.event.NormalizedListingEvent;
import com.jobsignal.shared.event.RawListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawListingConsumer {

    private final NormalizationService normalizationService;
    private final NormalizedListingProducer normalizedListingProducer;

    @KafkaListener(
            topics = "${kafka.topics.raw-listings}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(RawListingEvent event, Acknowledgment acknowledgment) {
        log.debug("Received raw listing source={} externalId={}",
                event.payload().source(), event.payload().externalId());

        NormalizedListingEvent normalized = normalizationService.normalize(event);
        normalizedListingProducer.publish(normalized);
        acknowledgment.acknowledge();

        log.info("Normalized listing source={} externalId={}",
                event.payload().source(), event.payload().externalId());
    }
}
