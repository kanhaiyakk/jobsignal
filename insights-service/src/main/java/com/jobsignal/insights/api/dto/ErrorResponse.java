package com.jobsignal.insights.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(description = "Machine-readable error code", example = "INSIGHT_ERROR") String code,
        @Schema(description = "Human-readable error message", example = "Failed to start report generation") String message
) {}
