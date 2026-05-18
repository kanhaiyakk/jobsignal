package com.jobsignal.insights.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Weekly job market insight report")
public record WeeklyReportResponse(
        @Schema(description = "Report unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Start of the analysis window", example = "2025-05-11")
        LocalDate weekStart,

        @Schema(description = "End of the analysis window", example = "2025-05-18")
        LocalDate weekEnd,

        @Schema(description = "Total job listings analyzed in this period", example = "512")
        int totalListings,

        @Schema(description = "Top technologies and skills ranked by frequency")
        List<SkillTrendResponse> topSkills,

        @Schema(description = "Count of listings by seniority level",
                example = "{\"SENIOR\": 210, \"MID\": 150, \"JUNIOR\": 50}")
        Map<String, Integer> seniorityCounts,

        @Schema(description = "Count of listings by remote policy",
                example = "{\"REMOTE\": 300, \"HYBRID\": 150, \"ON_SITE\": 62}")
        Map<String, Integer> remotePolicyCounts,

        @Schema(description = "Top hiring companies by listing volume")
        List<String> topCompanies,

        @Schema(description = "AI-generated narrative report summarizing market trends")
        String reportText,

        @Schema(description = "UTC timestamp when this report was generated")
        Instant generatedAt
) {}
