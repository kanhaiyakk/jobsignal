package com.jobsignal.insights.messaging.consumer;

import com.jobsignal.insights.service.ListingSnapshotService;
import com.jobsignal.shared.event.EnrichedListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrichedListingConsumer {

    private final ListingSnapshotService listingSnapshotService;

    @KafkaListener(
            topics = "${kafka.topics.enriched-listings}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(EnrichedListingEvent event, Acknowledgment acknowledgment) {
        log.debug("Received enriched listing source={} externalId={}",
                event.payload().source(), event.payload().externalId());
        listingSnapshotService.saveSnapshot(event);
        acknowledgment.acknowledge();
    }
}
