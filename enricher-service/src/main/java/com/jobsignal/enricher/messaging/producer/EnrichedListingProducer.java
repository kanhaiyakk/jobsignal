package com.jobsignal.enricher.messaging.producer;

import com.jobsignal.shared.event.EnrichedListingEvent;

public interface EnrichedListingProducer {
    void publish(EnrichedListingEvent event);
}
