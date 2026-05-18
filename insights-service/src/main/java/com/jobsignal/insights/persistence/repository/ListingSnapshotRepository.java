package com.jobsignal.insights.persistence.repository;

import com.jobsignal.insights.persistence.entity.ListingSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ListingSnapshotRepository extends JpaRepository<ListingSnapshotEntity, UUID> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    List<ListingSnapshotEntity> findByCreatedAtBetween(Instant from, Instant to);
}
