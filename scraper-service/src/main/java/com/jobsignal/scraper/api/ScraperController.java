package com.jobsignal.scraper.api;

import com.jobsignal.scraper.api.dto.ScrapeResultResponse;
import com.jobsignal.scraper.service.ListingScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scraper")
@RequiredArgsConstructor
@Tag(name = "Scraper", description = "Trigger ingestion from external job sources")
public class ScraperController {

    private final ListingScraperService scraperService;

    @Operation(
            summary = "Trigger RemoteOK scrape",
            description = "Fetches the latest job listings from RemoteOK and stores new ones. " +
                    "Already-existing listings are skipped (idempotent)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Scrape completed",
                    content = @Content(schema = @Schema(implementation = ScrapeResultResponse.class))),
            @ApiResponse(responseCode = "500", description = "Server error or RemoteOK unreachable")
    })
    @PostMapping("/trigger")
    public ResponseEntity<ScrapeResultResponse> triggerScrape() {
        ListingScraperService.ScrapeResult result = scraperService.scrapeRemoteOk();
        return ResponseEntity.ok(new ScrapeResultResponse(
                result.fetched(),
                result.saved(),
                result.skipped()
        ));
    }
}
