package com.jobsignal.storage.unit;

import com.jobsignal.shared.event.NormalizedListingEvent;
import com.jobsignal.storage.persistence.entity.NormalizedListingEntity;
import com.jobsignal.storage.persistence.repository.NormalizedListingRepository;
import com.jobsignal.storage.service.ListingStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingStorageServiceTest {

    @Mock
    private NormalizedListingRepository repository;

    @InjectMocks
    private ListingStorageService service;

    @Test
    void store_whenNewListing_persistsToDatabase() {
        NormalizedListingEvent event = buildEvent("remoteok", "ext-1");
        when(repository.existsBySourceAndExternalId("remoteok", "ext-1")).thenReturn(false);

        service.store(event);

        ArgumentCaptor<NormalizedListingEntity> captor =
                ArgumentCaptor.forClass(NormalizedListingEntity.class);
        verify(repository).save(captor.capture());

        NormalizedListingEntity saved = captor.getValue();
        assertThat(saved.getSource()).isEqualTo("remoteok");
        assertThat(saved.getExternalId()).isEqualTo("ext-1");
        assertThat(saved.getTitle()).isEqualTo("Senior Engineer");
        assertThat(saved.isRemote()).isTrue();
    }

    @Test
    void store_whenDuplicateListing_skipsWithoutSaving() {
        NormalizedListingEvent event = buildEvent("remoteok", "ext-dup");
        when(repository.existsBySourceAndExternalId("remoteok", "ext-dup")).thenReturn(true);

        service.store(event);

        verify(repository, never()).save(any());
    }

    @Test
    void store_setsAllFieldsFromPayload() {
        Instant postedAt = Instant.parse("2024-01-15T10:00:00Z");
        NormalizedListingEvent event = buildEventWithDetails("remoteok", "ext-99",
                "Staff Engineer", "FAANG Corp", "San Francisco, CA", false, postedAt);
        when(repository.existsBySourceAndExternalId(any(), any())).thenReturn(false);

        service.store(event);

        ArgumentCaptor<NormalizedListingEntity> captor =
                ArgumentCaptor.forClass(NormalizedListingEntity.class);
        verify(repository).save(captor.capture());

        NormalizedListingEntity saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Staff Engineer");
        assertThat(saved.getCompany()).isEqualTo("FAANG Corp");
        assertThat(saved.getLocation()).isEqualTo("San Francisco, CA");
        assertThat(saved.isRemote()).isFalse();
        assertThat(saved.getPostedAt()).isEqualTo(postedAt);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    private NormalizedListingEvent buildEvent(String source, String externalId) {
        return buildEventWithDetails(source, externalId,
                "Senior Engineer", "Acme Corp", "Remote", true, Instant.now());
    }

    private NormalizedListingEvent buildEventWithDetails(
            String source, String externalId,
            String title, String company, String location,
            boolean isRemote, Instant postedAt) {
        return new NormalizedListingEvent(
                UUID.randomUUID(), "LISTING_NORMALIZED", Instant.now(), 1,
                new NormalizedListingEvent.NormalizedListingPayload(
                        UUID.randomUUID().toString(),
                        source, externalId, title, company, location,
                        "Plain text description.", "https://apply.co",
                        postedAt, isRemote
                )
        );
    }
}
