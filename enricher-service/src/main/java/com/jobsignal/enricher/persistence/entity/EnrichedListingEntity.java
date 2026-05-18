package com.jobsignal.enricher.persistence.entity;

import com.jobsignal.enricher.model.ExtractionResult;
import com.jobsignal.enricher.persistence.converter.FloatArrayVectorType;
import com.jobsignal.shared.event.NormalizedListingEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "enriched_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrichedListingEntity {

    @Id
    private UUID id;

    @Column(name = "normalized_listing_id", length = 255)
    private String normalizedListingId;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(length = 500)
    private String title;

    @Column(length = 255)
    private String company;

    @Column(length = 255)
    private String location;

    @Column(name = "description_text", columnDefinition = "TEXT")
    private String descriptionText;

    @Column(name = "apply_url", length = 1000)
    private String applyUrl;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "is_remote", nullable = false)
    private boolean isRemote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tech_stack", columnDefinition = "jsonb")
    private List<String> techStack;

    @Column(length = 50)
    private String seniority;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "remote_policy", length = 50)
    private String remotePolicy;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Type(FloatArrayVectorType.class)
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static EnrichedListingEntity from(
            NormalizedListingEvent.NormalizedListingPayload payload,
            ExtractionResult extraction,
            float[] embedding,
            String contentHash) {
        var entity = new EnrichedListingEntity();
        entity.id = UUID.randomUUID();
        entity.normalizedListingId = payload.rawListingId();
        entity.source = payload.source();
        entity.externalId = payload.externalId();
        entity.title = payload.title();
        entity.company = payload.company();
        entity.location = payload.location();
        entity.descriptionText = payload.descriptionText();
        entity.applyUrl = payload.applyUrl();
        entity.postedAt = payload.postedAt();
        entity.isRemote = payload.isRemote();
        entity.techStack = extraction.techStack();
        entity.seniority = extraction.seniority();
        entity.salaryRange = extraction.salaryRange();
        entity.experienceYears = extraction.experienceYears();
        entity.remotePolicy = extraction.remotePolicy();
        entity.contentHash = contentHash;
        entity.embedding = embedding;
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }
}
