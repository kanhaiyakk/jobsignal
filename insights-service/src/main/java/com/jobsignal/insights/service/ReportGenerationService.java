package com.jobsignal.insights.service;

import com.jobsignal.insights.client.GeminiClient;
import com.jobsignal.insights.model.InsightSummary;
import com.jobsignal.insights.model.SkillCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportGenerationService {

    private final GeminiClient geminiClient;
    private final String promptTemplate;

    public ReportGenerationService(
            GeminiClient geminiClient,
            @Value("classpath:prompts/weekly-report-v1.txt") Resource promptResource
    ) throws IOException {
        this.geminiClient = geminiClient;
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public String generateReport(InsightSummary summary) {
        String prompt = buildPrompt(summary);
        log.info("Requesting Gemini report for week={} listings={}", summary.weekStart(), summary.totalListings());
        return geminiClient.generateReport(prompt);
    }

    private String buildPrompt(InsightSummary summary) {
        return promptTemplate
                .replace("{week_start}", summary.weekStart().toString())
                .replace("{week_end}", summary.weekEnd().toString())
                .replace("{total_listings}", String.valueOf(summary.totalListings()))
                .replace("{skills_summary}", formatSkills(summary))
                .replace("{seniority_summary}", formatMap(summary.seniorityCounts()))
                .replace("{remote_policy_summary}", formatMap(summary.remotePolicyCounts()))
                .replace("{companies_summary}", String.join(", ", summary.topCompanies()));
    }

    private String formatSkills(InsightSummary summary) {
        return summary.topSkills().stream()
                .map(s -> s.skill() + " (" + s.count() + ")")
                .collect(Collectors.joining(", "));
    }

    private String formatMap(java.util.Map<String, Integer> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", "));
    }
}
