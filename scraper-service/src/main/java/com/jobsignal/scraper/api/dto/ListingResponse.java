package com.jobsignal.scraper.api.dto;

import com.jobsignal.scraper.persistence.entity.RawListingEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A raw job listing as ingested from a source")
public record ListingResponse(

        @Schema(description = "Unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Source system", example = "remoteok")
        String source,

        @Schema(description = "Identifier from the source system", example = "12345")
        String externalId,

        @Schema(description = "Job title", example = "Senior Java Backend Engineer")
        String title,

        @Schema(description = "Company name", example = "Acme Corp")
        String company,

        @Schema(description = "Job location", example = "Remote, USA")
        String location,

        @Schema(description = "URL to apply for this job", example = "https://example.com/apply")
        String applyUrl,

        @Schema(description = "When the listing was originally posted")
        Instant postedAt,

        @Schema(description = "When this record was created in our system")
        Instant createdAt
) {
    public static ListingResponse from(RawListingEntity entity) {
        return new ListingResponse(
                entity.getId(),
                entity.getSource(),
                entity.getExternalId(),
                entity.getTitle(),
                entity.getCompany(),
                entity.getLocation(),
                entity.getApplyUrl(),
                entity.getPostedAt(),
                entity.getCreatedAt()
        );
    }
}
