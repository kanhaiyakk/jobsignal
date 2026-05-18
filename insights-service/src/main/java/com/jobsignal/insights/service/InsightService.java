package com.jobsignal.insights.service;

import com.jobsignal.insights.api.dto.SkillTrendResponse;
import com.jobsignal.insights.api.dto.WeeklyReportResponse;
import com.jobsignal.insights.exception.InsightException;
import com.jobsignal.insights.persistence.entity.WeeklyReportEntity;
import com.jobsignal.insights.persistence.repository.WeeklyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightService {

    private final WeeklyReportRepository reportRepository;
    private final JobLauncher jobLauncher;
    private final Job weeklyReportJob;

    public Page<WeeklyReportResponse> listReports(Pageable pageable) {
        return reportRepository.findAllByOrderByWeekEndDesc(pageable).map(this::toResponse);
    }

    public Optional<WeeklyReportResponse> latestReport() {
        return reportRepository.findTopByOrderByWeekEndDesc().map(this::toResponse);
    }

    public void triggerGeneration() {
        try {
            var params = new JobParametersBuilder()
                    .addLocalDateTime("runAt", LocalDateTime.now())
                    .toJobParameters();
            jobLauncher.run(weeklyReportJob, params);
            log.info("Weekly report job launched");
        } catch (Exception e) {
            throw new InsightException("Failed to start report generation: " + e.getMessage(), e);
        }
    }

    private WeeklyReportResponse toResponse(WeeklyReportEntity entity) {
        List<SkillTrendResponse> topSkills = entity.getTopSkills() == null ? List.of() :
                entity.getTopSkills().stream()
                        .map(s -> new SkillTrendResponse(s.skill(), s.count()))
                        .toList();
        return new WeeklyReportResponse(
                entity.getId(), entity.getWeekStart(), entity.getWeekEnd(),
                entity.getTotalListings(), topSkills, entity.getSeniorityCounts(),
                entity.getRemotePolicyCounts(), entity.getTopCompanies(),
                entity.getReportText(), entity.getCreatedAt());
    }
}
