package com.jobsignal.enricher.persistence.repository;

import com.jobsignal.enricher.persistence.entity.EnrichedListingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EnrichedListingRepository extends JpaRepository<EnrichedListingEntity, UUID> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM enriched_listings
                WHERE embedding <=> CAST(:queryVector AS vector) < :maxDistance
            )
            """, nativeQuery = true)
    boolean existsNearDuplicate(@Param("queryVector") String queryVector,
                                @Param("maxDistance") double maxDistance);
}
