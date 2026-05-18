package com.jobsignal.insights.api;

import com.jobsignal.insights.api.dto.ErrorResponse;
import com.jobsignal.insights.api.dto.WeeklyReportResponse;
import com.jobsignal.insights.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "Weekly job market trend reports and analytics")
public class InsightsController {

    private final InsightService insightService;

    @GetMapping
    @Operation(
            summary = "List weekly reports",
            description = "Returns a paginated list of generated weekly insight reports, newest first")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<WeeklyReportResponse>> listReports(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of reports per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(insightService.listReports(PageRequest.of(page, size)));
    }

    @GetMapping("/latest")
    @Operation(
            summary = "Get latest report",
            description = "Returns the most recently generated weekly insight report")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report found"),
            @ApiResponse(responseCode = "404", description = "No reports have been generated yet"),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<WeeklyReportResponse> getLatestReport() {
        return insightService.latestReport()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Trigger report generation",
            description = "Manually triggers a weekly insight report generation job")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Report generation started"),
            @ApiResponse(responseCode = "500", description = "Failed to start report generation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> generateReport() {
        insightService.triggerGeneration();
        return ResponseEntity.accepted().build();
    }
}
