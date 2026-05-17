package com.jobsignal.normalizer.messaging.producer;

import com.jobsignal.shared.event.NormalizedListingEvent;

public interface NormalizedListingProducer {
    void publish(NormalizedListingEvent event);
}
