package com.jobsignal.insights.unit;

import com.jobsignal.insights.persistence.entity.ListingSnapshotEntity;
import com.jobsignal.insights.persistence.repository.ListingSnapshotRepository;
import com.jobsignal.insights.service.ListingSnapshotService;
import com.jobsignal.shared.event.EnrichedListingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingSnapshotServiceTest {

    @Mock
    private ListingSnapshotRepository repository;

    @InjectMocks
    private ListingSnapshotService service;

    @Test
    void saveSnapshot_whenNewListing_persistsEntity() {
        EnrichedListingEvent event = buildEvent("remoteok", "ext-1");
        when(repository.existsBySourceAndExternalId("remoteok", "ext-1")).thenReturn(false);

        service.saveSnapshot(event);

        ArgumentCaptor<ListingSnapshotEntity> captor = ArgumentCaptor.forClass(ListingSnapshotEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo("remoteok");
        assertThat(captor.getValue().getExternalId()).isEqualTo("ext-1");
        assertThat(captor.getValue().getSeniority()).isEqualTo("SENIOR");
    }

    @Test
    void saveSnapshot_whenDuplicate_skipsWithoutPersisting() {
        EnrichedListingEvent event = buildEvent("remoteok", "ext-dup");
        when(repository.existsBySourceAndExternalId("remoteok", "ext-dup")).thenReturn(true);

        service.saveSnapshot(event);

        verify(repository, never()).save(any());
    }

    private EnrichedListingEvent buildEvent(String source, String externalId) {
        return new EnrichedListingEvent(
                UUID.randomUUID(), "LISTING_ENRICHED", Instant.now(), 1,
                new EnrichedListingEvent.EnrichedListingPayload(
                        UUID.randomUUID().toString(),
                        source, externalId,
                        "Senior Java Engineer", "Acme Corp", "Remote",
                        "We are hiring a Java engineer.", "https://apply.co",
                        Instant.now(), true,
                        List.of("Java", "Spring Boot"), "SENIOR", "$120k-$150k", 5, "REMOTE"
                )
        );
    }
}
