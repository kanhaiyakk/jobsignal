package com.jobsignal.shared.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InsightGeneratedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        InsightGeneratedPayload payload
) {
    public record InsightGeneratedPayload(
            UUID reportId,
            LocalDate weekStart,
            LocalDate weekEnd,
            int totalListings
    ) {}
}
