package com.jobsignal.scraper.persistence.repository;

import com.jobsignal.scraper.persistence.entity.RawListingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RawListingRepository extends JpaRepository<RawListingEntity, UUID> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    @Query("SELECT r FROM RawListingEntity r WHERE r.source = :source ORDER BY r.createdAt DESC")
    Page<RawListingEntity> findBySource(@Param("source") String source, Pageable pageable);

    @Query("SELECT r FROM RawListingEntity r ORDER BY r.createdAt DESC")
    Page<RawListingEntity> findAllOrderedByCreatedAt(Pageable pageable);

    @Query("SELECT r FROM RawListingEntity r WHERE r.id = :id")
    Optional<RawListingEntity> findByIdStrict(@Param("id") UUID id);
}
