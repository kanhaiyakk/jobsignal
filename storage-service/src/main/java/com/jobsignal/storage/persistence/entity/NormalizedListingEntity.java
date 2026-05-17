package com.jobsignal.storage.persistence.entity;

import com.jobsignal.shared.event.NormalizedListingEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "normalized_listings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NormalizedListingEntity {

    @Id
    private UUID id;

    @Column(name = "raw_listing_id", length = 255)
    private String rawListingId;

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

    @Column(name = "description_text", columnDefinition = "TEXT")
    private String descriptionText;

    @Column(name = "apply_url", length = 1000)
    private String applyUrl;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "is_remote", nullable = false)
    private boolean isRemote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static NormalizedListingEntity from(NormalizedListingEvent.NormalizedListingPayload payload) {
        var entity = new NormalizedListingEntity();
        entity.id = UUID.randomUUID();
        entity.rawListingId = payload.rawListingId();
        entity.source = payload.source();
        entity.externalId = payload.externalId();
        entity.title = payload.title();
        entity.company = payload.company();
        entity.location = payload.location();
        entity.descriptionText = payload.descriptionText();
        entity.applyUrl = payload.applyUrl();
        entity.postedAt = payload.postedAt();
        entity.isRemote = payload.isRemote();
        entity.createdAt = Instant.now();
        entity.updatedAt = Instant.now();
        return entity;
    }
}
