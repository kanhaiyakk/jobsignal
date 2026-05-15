package com.jobsignal.scraper.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a scrape operation")
public record ScrapeResultResponse(

        @Schema(description = "Total listings fetched from source", example = "87")
        int fetched,

        @Schema(description = "New listings saved to database", example = "12")
        int saved,

        @Schema(description = "Listings skipped because they already existed", example = "75")
        int skipped
) {}
