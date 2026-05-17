package com.jobsignal.storage.service;

import com.jobsignal.shared.event.NormalizedListingEvent;
import com.jobsignal.storage.persistence.entity.NormalizedListingEntity;
import com.jobsignal.storage.persistence.repository.NormalizedListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingStorageService {

    private final NormalizedListingRepository repository;

    @Transactional
    public void store(NormalizedListingEvent event) {
        NormalizedListingEvent.NormalizedListingPayload payload = event.payload();

        if (repository.existsBySourceAndExternalId(payload.source(), payload.externalId())) {
            log.debug("Duplicate normalized listing skipped source={} externalId={}",
                    payload.source(), payload.externalId());
            return;
        }

        NormalizedListingEntity entity = NormalizedListingEntity.from(payload);
        repository.save(entity);
        log.info("Stored normalized listing source={} externalId={}",
                payload.source(), payload.externalId());
    }
}
