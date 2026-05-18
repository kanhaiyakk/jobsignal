package com.jobsignal.insights.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportScheduler {

    private final InsightService insightService;

    @Scheduled(cron = "${insights.report-cron}")
    public void runWeeklyReport() {
        log.info("Scheduled weekly report job triggered");
        insightService.triggerGeneration();
    }
}
