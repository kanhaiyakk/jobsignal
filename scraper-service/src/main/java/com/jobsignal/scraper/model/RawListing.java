package com.jobsignal.scraper.model;

import java.time.Instant;
import java.util.List;

public record RawListing(
        String externalId,
        String source,
        String title,
        String company,
        String location,
        String description,
        String applyUrl,
        Instant postedAt,
        List<String> tags,
        String rawPayload
) {}