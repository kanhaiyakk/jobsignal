package com.jobsignal.insights.persistence.entity;

import com.jobsignal.shared.event.EnrichedListingEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listing_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListingSnapshotEntity {

    @Id
    private UUID id;

    @Column(name = "enriched_event_id", nullable = false, length = 255)
    private String enrichedEventId;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(length = 500)
    private String title;

    @Column(length = 255)
    private String company;

    @Column(length = 50)
    private String seniority;

    @Column(name = "remote_policy", length = 50)
    private String remotePolicy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tech_stack", columnDefinition = "jsonb")
    private List<String> techStack;

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "is_remote", nullable = false)
    private boolean isRemote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ListingSnapshotEntity from(EnrichedListingEvent event) {
        var payload = event.payload();
        var entity = new ListingSnapshotEntity();
        entity.id = UUID.randomUUID();
        entity.enrichedEventId = event.eventId().toString();
        entity.source = payload.source();
        entity.externalId = payload.externalId();
        entity.title = payload.title();
        entity.company = payload.company();
        entity.seniority = payload.seniority();
        entity.remotePolicy = payload.remotePolicy();
        entity.techStack = payload.techStack() != null ? payload.techStack() : List.of();
        entity.salaryRange = payload.salaryRange();
        entity.experienceYears = payload.experienceYears();
        entity.isRemote = payload.isRemote();
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }
}
