package com.jobsignal.scraper.messaging.producer;

import com.jobsignal.shared.event.RawListingEvent;

public interface RawListingProducer {
    void publish(RawListingEvent event);
}
