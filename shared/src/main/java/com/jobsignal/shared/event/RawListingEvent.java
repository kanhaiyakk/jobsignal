package com.jobsignal.shared.event;

import java.time.Instant;
import java.util.UUID;

public record RawListingEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        RawListingPayload payload
) {
    public record RawListingPayload(
            String id,
            String source,
            String externalId,
            String title,
            String company,
            String location,
            String description,
            String applyUrl,
            Instant postedAt,
            String rawPayload
    ) {}
}
