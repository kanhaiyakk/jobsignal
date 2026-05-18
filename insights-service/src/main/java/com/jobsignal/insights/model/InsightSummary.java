package com.jobsignal.insights.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record InsightSummary(
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalListings,
        List<SkillCount> topSkills,
        Map<String, Integer> seniorityCounts,
        Map<String, Integer> remotePolicyCounts,
        List<String> topCompanies
) {}
