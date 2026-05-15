package com.jobsignal.scraper.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Paginated list of results")
public record PagedResponse<T>(

        @Schema(description = "Items on the current page")
        List<T> content,

        @Schema(description = "Current page number (zero-based)", example = "0")
        int page,

        @Schema(description = "Number of items per page", example = "20")
        int size,

        @Schema(description = "Total number of items across all pages", example = "100")
        long totalElements,

        @Schema(description = "Total number of pages", example = "5")
        int totalPages,

        @Schema(description = "Whether this is the last page", example = "false")
        boolean last
) {
    public static <E, R> PagedResponse<R> from(Page<E> page, Function<E, R> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
