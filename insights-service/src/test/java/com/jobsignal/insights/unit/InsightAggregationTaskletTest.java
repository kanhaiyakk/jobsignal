package com.jobsignal.insights.unit;

import com.jobsignal.insights.config.InsightsProperties;
import com.jobsignal.insights.messaging.producer.InsightEventProducer;
import com.jobsignal.insights.persistence.entity.ListingSnapshotEntity;
import com.jobsignal.insights.persistence.entity.WeeklyReportEntity;
import com.jobsignal.insights.persistence.repository.ListingSnapshotRepository;
import com.jobsignal.insights.persistence.repository.WeeklyReportRepository;
import com.jobsignal.insights.service.InsightAggregationTasklet;
import com.jobsignal.insights.service.ReportGenerationService;
import com.jobsignal.shared.event.EnrichedListingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightAggregationTaskletTest {

    @Mock private ListingSnapshotRepository snapshotRepository;
    @Mock private WeeklyReportRepository reportRepository;
    @Mock private ReportGenerationService reportGenerationService;
    @Mock private InsightEventProducer eventProducer;

    private InsightAggregationTasklet tasklet;

    @BeforeEach
    void setUp() {
        InsightsProperties props = new InsightsProperties(7, "0 0 8 * * MON");
        tasklet = new InsightAggregationTasklet(
                snapshotRepository, reportRepository, reportGenerationService, eventProducer, props);
    }

    @Test
    void execute_whenListingsAvailable_generatesAndPersistsReport() throws Exception {
        List<ListingSnapshotEntity> snapshots = List.of(
                buildSnapshot("Java", "Spring Boot", "SENIOR", "REMOTE"),
                buildSnapshot("Python", "FastAPI", "MID", "HYBRID"));
        when(snapshotRepository.findByCreatedAtBetween(any(), any())).thenReturn(snapshots);
        when(reportGenerationService.generateReport(any())).thenReturn("Strong hiring week in tech.");
        when(reportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        ArgumentCaptor<WeeklyReportEntity> captor = ArgumentCaptor.forClass(WeeklyReportEntity.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalListings()).isEqualTo(2);
        assertThat(captor.getValue().getReportText()).isEqualTo("Strong hiring week in tech.");
        verify(eventProducer).publish(any());
    }

    @Test
    void execute_whenNoListings_skipsReportGeneration() throws Exception {
        when(snapshotRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of());

        RepeatStatus status = tasklet.execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(reportRepository, never()).save(any());
        verify(eventProducer, never()).publish(any());
    }

    private ListingSnapshotEntity buildSnapshot(String skill1, String skill2,
                                                String seniority, String remotePolicy) {
        EnrichedListingEvent event = new EnrichedListingEvent(
                UUID.randomUUID(), "LISTING_ENRICHED", Instant.now(), 1,
                new EnrichedListingEvent.EnrichedListingPayload(
                        UUID.randomUUID().toString(), "remoteok", UUID.randomUUID().toString(),
                        "Senior Engineer", "Acme Corp", "Remote", "Job description",
                        "https://apply.co", Instant.now(), true,
                        List.of(skill1, skill2), seniority, "$100k", 3, remotePolicy
                )
        );
        return ListingSnapshotEntity.from(event);
    }
}
