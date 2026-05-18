package com.jobsignal.insights.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Skill demand data point")
public record SkillTrendResponse(
        @Schema(description = "Technology or skill name", example = "Java") String skill,
        @Schema(description = "Number of listings requiring this skill", example = "42") int count
) {}
