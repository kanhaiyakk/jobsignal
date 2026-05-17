package com.jobsignal.shared.event;

import java.time.Instant;
import java.util.UUID;

public record FailedListingEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        String originTopic,
        String errorMessage,
        String rawPayload
) {}
