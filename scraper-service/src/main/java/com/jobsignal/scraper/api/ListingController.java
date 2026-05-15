package com.jobsignal.scraper.api;

import com.jobsignal.scraper.api.dto.ListingResponse;
import com.jobsignal.scraper.api.dto.PagedResponse;
import com.jobsignal.scraper.exception.ListingNotFoundException;
import com.jobsignal.scraper.service.ListingScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
@Tag(name = "Listings", description = "Browse ingested job listings")
public class ListingController {

    private final ListingScraperService scraperService;

    @Operation(
            summary = "List all listings",
            description = "Returns a paginated list of raw job listings ordered by ingestion date descending"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<ListingResponse>> listListings(
            @Parameter(description = "Page number (zero-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        int clampedSize = Math.min(size, 100);
        var pageable = PageRequest.of(page, clampedSize, Sort.by("createdAt").descending());
        var result = scraperService.listAllListings(pageable);
        return ResponseEntity.ok(PagedResponse.from(result, ListingResponse::from));
    }

    @Operation(
            summary = "Get listing by ID",
            description = "Returns a single job listing by its unique identifier"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success",
                    content = @Content(schema = @Schema(implementation = ListingResponse.class))),
            @ApiResponse(responseCode = "404", description = "Listing not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListing(
            @Parameter(description = "Listing UUID", required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id
    ) {
        return scraperService.findById(id)
                .map(entity -> ResponseEntity.ok(ListingResponse.from(entity)))
                .orElseThrow(() -> new ListingNotFoundException(id));
    }
}
