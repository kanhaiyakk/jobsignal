package com.jobsignal.scraper.service;

import com.jobsignal.scraper.client.RemoteOkClient;
import com.jobsignal.scraper.messaging.producer.RawListingProducer;
import com.jobsignal.scraper.model.RawListing;
import com.jobsignal.scraper.persistence.entity.RawListingEntity;
import com.jobsignal.scraper.persistence.repository.RawListingRepository;
import com.jobsignal.shared.event.RawListingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ListingScraperService {

    private final RemoteOkClient remoteOkClient;
    private final RawListingRepository repository;
    private final RawListingProducer rawListingProducer;
    private final ScraperRateLimiter rateLimiter;

    @Transactional
    public ScrapeResult scrapeRemoteOk() {
        if (!rateLimiter.tryAcquire("remoteok")) {
            log.warn("Rate limit exceeded for remoteok - skipping scrape run");
            return new ScrapeResult(0, 0, 0);
        }

        List<RawListing> fetched = remoteOkClient.fetchLatestListings();

        int savedCount = 0;
        int skippedCount = 0;

        for (RawListing listing : fetched) {
            if (repository.existsBySourceAndExternalId(listing.source(), listing.externalId())) {
                skippedCount++;
                continue;
            }
            RawListingEntity entity = RawListingEntity.create(
                    listing.source(),
                    listing.externalId(),
                    listing.title(),
                    listing.company(),
                    listing.location(),
                    listing.description(),
                    listing.applyUrl(),
                    listing.postedAt(),
                    listing.rawPayload()
            );
            repository.save(entity);
            rawListingProducer.publish(toEvent(entity));
            savedCount++;
        }

        log.info("Scrape complete: fetched={} saved={} skipped={}", fetched.size(), savedCount, skippedCount);
        return new ScrapeResult(fetched.size(), savedCount, skippedCount);
    }

    @Transactional(readOnly = true)
    public Page<RawListingEntity> listAllListings(Pageable pageable) {
        return repository.findAllOrderedByCreatedAt(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<RawListingEntity> findById(UUID id) {
        return repository.findByIdStrict(id);
    }

    private RawListingEvent toEvent(RawListingEntity entity) {
        return new RawListingEvent(
                UUID.randomUUID(),
                "RAW_LISTING_SCRAPED",
                Instant.now(),
                1,
                new RawListingEvent.RawListingPayload(
                        entity.getId().toString(),
                        entity.getSource(),
                        entity.getExternalId(),
                        entity.getTitle(),
                        entity.getCompany(),
                        entity.getLocation(),
                        entity.getDescription(),
                        entity.getApplyUrl(),
                        entity.getPostedAt(),
                        entity.getRawPayload()
                )
        );
    }

    public record ScrapeResult(int fetched, int saved, int skipped) {}
}
