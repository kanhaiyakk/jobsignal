package com.jobsignal.scraper.persistence.entity;

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
import java.util.UUID;

@Entity
@Table(name = "raw_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawListingEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 255)
    private String company;

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "apply_url", length = 1000)
    private String applyUrl;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static RawListingEntity create(
            String source,
            String externalId,
            String title,
            String company,
            String location,
            String description,
            String applyUrl,
            Instant postedAt,
            String rawPayload) {

        var entity = new RawListingEntity();
        entity.id = UUID.randomUUID();
        entity.source = source;
        entity.externalId = externalId;
        entity.title = title;
        entity.company = company;
        entity.location = location;
        entity.description = description;
        entity.applyUrl = applyUrl;
        entity.postedAt = postedAt;
        entity.rawPayload = rawPayload;
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }
}
