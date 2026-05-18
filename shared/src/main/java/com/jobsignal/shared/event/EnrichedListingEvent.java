package com.jobsignal.shared.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EnrichedListingEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        EnrichedListingPayload payload
) {
    public record EnrichedListingPayload(
            String rawListingId,
            String source,
            String externalId,
            String title,
            String company,
            String location,
            String descriptionText,
            String applyUrl,
            Instant postedAt,
            boolean isRemote,
            List<String> techStack,
            String seniority,
            String salaryRange,
            Integer experienceYears,
            String remotePolicy
    ) {}
}
