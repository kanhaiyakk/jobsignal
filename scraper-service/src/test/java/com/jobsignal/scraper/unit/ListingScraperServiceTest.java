package com.jobsignal.scraper.unit;

import com.jobsignal.scraper.client.RemoteOkClient;
import com.jobsignal.scraper.messaging.producer.RawListingProducer;
import com.jobsignal.scraper.model.RawListing;
import com.jobsignal.scraper.persistence.entity.RawListingEntity;
import com.jobsignal.scraper.persistence.repository.RawListingRepository;
import com.jobsignal.scraper.service.ListingScraperService;
import com.jobsignal.scraper.service.ScraperRateLimiter;
import com.jobsignal.shared.event.RawListingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingScraperServiceTest {

    @Mock
    private RemoteOkClient remoteOkClient;

    @Mock
    private RawListingRepository repository;

    @Mock
    private RawListingProducer rawListingProducer;

    @Mock
    private ScraperRateLimiter rateLimiter;

    @InjectMocks
    private ListingScraperService service;

    @BeforeEach
    void setUp() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);
    }

    @Test
    void scrapeRemoteOk_whenNewListings_savesAllAndPublishesEvents() {
        var listing1 = buildListing("ext-1");
        var listing2 = buildListing("ext-2");

        when(remoteOkClient.fetchLatestListings()).thenReturn(List.of(listing1, listing2));
        when(repository.existsBySourceAndExternalId(eq("remoteok"), eq("ext-1"))).thenReturn(false);
        when(repository.existsBySourceAndExternalId(eq("remoteok"), eq("ext-2"))).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ListingScraperService.ScrapeResult result = service.scrapeRemoteOk();

        assertThat(result.fetched()).isEqualTo(2);
        assertThat(result.saved()).isEqualTo(2);
        assertThat(result.skipped()).isEqualTo(0);
        verify(repository, times(2)).save(any(RawListingEntity.class));
        verify(rawListingProducer, times(2)).publish(any(RawListingEvent.class));
    }

    @Test
    void scrapeRemoteOk_whenListingAlreadyExists_skipsItAndDoesNotPublish() {
        var listing = buildListing("ext-existing");

        when(remoteOkClient.fetchLatestListings()).thenReturn(List.of(listing));
        when(repository.existsBySourceAndExternalId("remoteok", "ext-existing")).thenReturn(true);

        ListingScraperService.ScrapeResult result = service.scrapeRemoteOk();

        assertThat(result.fetched()).isEqualTo(1);
        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        verify(repository, never()).save(any());
        verify(rawListingProducer, never()).publish(any());
    }

    @Test
    void scrapeRemoteOk_whenClientReturnsEmpty_returnsZeroCounts() {
        when(remoteOkClient.fetchLatestListings()).thenReturn(List.of());

        ListingScraperService.ScrapeResult result = service.scrapeRemoteOk();

        assertThat(result.fetched()).isEqualTo(0);
        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(0);
        verify(repository, never()).save(any());
        verify(rawListingProducer, never()).publish(any());
    }

    @Test
    void scrapeRemoteOk_whenRateLimitExceeded_returnsZeroCountsWithoutScraping() {
        when(rateLimiter.tryAcquire("remoteok")).thenReturn(false);

        ListingScraperService.ScrapeResult result = service.scrapeRemoteOk();

        assertThat(result.fetched()).isEqualTo(0);
        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(0);
        verify(remoteOkClient, never()).fetchLatestListings();
        verify(rawListingProducer, never()).publish(any());
    }

    @Test
    void scrapeRemoteOk_savesEntityWithCorrectFields() {
        var listing = buildListing("ext-99");

        when(remoteOkClient.fetchLatestListings()).thenReturn(List.of(listing));
        when(repository.existsBySourceAndExternalId(any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.scrapeRemoteOk();

        ArgumentCaptor<RawListingEntity> captor = ArgumentCaptor.forClass(RawListingEntity.class);
        verify(repository).save(captor.capture());

        RawListingEntity saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo("remoteok");
        assertThat(saved.getExternalId()).isEqualTo("ext-99");
        assertThat(saved.getTitle()).isEqualTo("Senior Java Engineer");
    }

    private RawListing buildListing(String externalId) {
        return new RawListing(
                externalId,
                "remoteok",
                "Senior Java Engineer",
                "Acme Corp",
                "Remote",
                "We are looking for...",
                "https://example.com/apply",
                Instant.now(),
                List.of("java", "spring"),
                "{}"
        );
    }
}
