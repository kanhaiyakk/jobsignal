package com.jobsignal.storage.persistence.repository;

import com.jobsignal.storage.persistence.entity.NormalizedListingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NormalizedListingRepository extends JpaRepository<NormalizedListingEntity, UUID> {

    boolean existsBySourceAndExternalId(String source, String externalId);
}
