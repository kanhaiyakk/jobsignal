package com.jobsignal.insights.service;

import com.jobsignal.insights.config.InsightsProperties;
import com.jobsignal.insights.messaging.producer.InsightEventProducer;
import com.jobsignal.insights.model.InsightSummary;
import com.jobsignal.insights.model.SkillCount;
import com.jobsignal.insights.persistence.entity.ListingSnapshotEntity;
import com.jobsignal.insights.persistence.entity.WeeklyReportEntity;
import com.jobsignal.insights.persistence.repository.ListingSnapshotRepository;
import com.jobsignal.insights.persistence.repository.WeeklyReportRepository;
import com.jobsignal.shared.event.InsightGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InsightAggregationTasklet implements Tasklet {

    private final ListingSnapshotRepository snapshotRepository;
    private final WeeklyReportRepository reportRepository;
    private final ReportGenerationService reportGenerationService;
    private final InsightEventProducer eventProducer;
    private final InsightsProperties properties;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LocalDate weekEnd = LocalDate.now(ZoneOffset.UTC);
        LocalDate weekStart = weekEnd.minusDays(properties.lookbackDays());
        Instant from = weekStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = weekEnd.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<ListingSnapshotEntity> snapshots = snapshotRepository.findByCreatedAtBetween(from, to);
        if (snapshots.isEmpty()) {
            log.info("No listings in window weekStart={} weekEnd={}, skipping report", weekStart, weekEnd);
            return RepeatStatus.FINISHED;
        }

        InsightSummary summary = aggregate(snapshots, weekStart, weekEnd);
        String reportText = reportGenerationService.generateReport(summary);
        WeeklyReportEntity report = WeeklyReportEntity.from(summary, reportText);
        reportRepository.save(report);

        eventProducer.publish(new InsightGeneratedEvent(
                UUID.randomUUID(), "INSIGHT_GENERATED", Instant.now(), 1,
                new InsightGeneratedEvent.InsightGeneratedPayload(
                        report.getId(), weekStart, weekEnd, summary.totalListings())));

        log.info("Weekly report generated reportId={} totalListings={}", report.getId(), summary.totalListings());
        return RepeatStatus.FINISHED;
    }

    private InsightSummary aggregate(List<ListingSnapshotEntity> snapshots,
                                     LocalDate weekStart, LocalDate weekEnd) {
        return new InsightSummary(
                weekStart, weekEnd, snapshots.size(),
                aggregateTopSkills(snapshots),
                aggregateSeniority(snapshots),
                aggregateRemotePolicy(snapshots),
                aggregateTopCompanies(snapshots));
    }

    private List<SkillCount> aggregateTopSkills(List<ListingSnapshotEntity> snapshots) {
        return snapshots.stream()
                .flatMap(s -> s.getTechStack().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> new SkillCount(e.getKey(), e.getValue().intValue()))
                .toList();
    }

    private Map<String, Integer> aggregateSeniority(List<ListingSnapshotEntity> snapshots) {
        return snapshots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSeniority() != null ? s.getSeniority() : "UNKNOWN",
                        Collectors.summingInt(x -> 1)));
    }

    private Map<String, Integer> aggregateRemotePolicy(List<ListingSnapshotEntity> snapshots) {
        return snapshots.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getRemotePolicy() != null ? s.getRemotePolicy() : "UNKNOWN",
                        Collectors.summingInt(x -> 1)));
    }

    private List<String> aggregateTopCompanies(List<ListingSnapshotEntity> snapshots) {
        return snapshots.stream()
                .filter(s -> s.getCompany() != null)
                .collect(Collectors.groupingBy(ListingSnapshotEntity::getCompany, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }
}
