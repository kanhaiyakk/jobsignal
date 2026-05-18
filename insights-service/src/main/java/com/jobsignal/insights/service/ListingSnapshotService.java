package com.jobsignal.insights.service;

import com.jobsignal.insights.persistence.entity.ListingSnapshotEntity;
import com.jobsignal.insights.persistence.repository.ListingSnapshotRepository;
import com.jobsignal.shared.event.EnrichedListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingSnapshotService {

    private final ListingSnapshotRepository repository;

    @Transactional
    public void saveSnapshot(EnrichedListingEvent event) {
        var payload = event.payload();
        if (repository.existsBySourceAndExternalId(payload.source(), payload.externalId())) {
            log.debug("Snapshot already exists source={} externalId={}, skipping",
                    payload.source(), payload.externalId());
            return;
        }
        repository.save(ListingSnapshotEntity.from(event));
        log.debug("Saved listing snapshot source={} externalId={}", payload.source(), payload.externalId());
    }
}
