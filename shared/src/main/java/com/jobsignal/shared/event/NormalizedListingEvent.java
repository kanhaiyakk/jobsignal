package com.jobsignal.shared.event;

import java.time.Instant;
import java.util.UUID;

public record NormalizedListingEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        NormalizedListingPayload payload
) {
    public record NormalizedListingPayload(
            String rawListingId,
            String source,
            String externalId,
            String title,
            String company,
            String location,
            String descriptionText,
            String applyUrl,
            Instant postedAt,
            boolean isRemote
    ) {}
}
