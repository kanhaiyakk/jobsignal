package com.jobsignal.insights.messaging.producer;

import com.jobsignal.shared.event.InsightGeneratedEvent;

public interface InsightEventProducer {
    void publish(InsightGeneratedEvent event);
}
