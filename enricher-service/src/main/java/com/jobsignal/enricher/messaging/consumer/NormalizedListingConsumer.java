package com.jobsignal.enricher.messaging.consumer;

import com.jobsignal.enricher.service.EnrichmentService;
import com.jobsignal.shared.event.NormalizedListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NormalizedListingConsumer {

    private final EnrichmentService enrichmentService;

    @KafkaListener(
            topics = "${kafka.topics.normalized-listings}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(NormalizedListingEvent event, Acknowledgment acknowledgment) {
        log.debug("Received normalized listing source={} externalId={}",
                event.payload().source(), event.payload().externalId());
        enrichmentService.enrich(event);
        acknowledgment.acknowledge();
    }
}
