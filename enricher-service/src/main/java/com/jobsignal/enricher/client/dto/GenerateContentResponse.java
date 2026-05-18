package com.jobsignal.enricher.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GenerateContentResponse(
        @JsonProperty("candidates") List<Candidate> candidates,
        @JsonProperty("usageMetadata") UsageMetadata usageMetadata
) {
    public record Candidate(@JsonProperty("content") Content content) {}
    public record Content(@JsonProperty("parts") List<Part> parts) {}
    public record Part(@JsonProperty("text") String text) {}

    public record UsageMetadata(
            @JsonProperty("promptTokenCount") int promptTokenCount,
            @JsonProperty("candidatesTokenCount") int candidatesTokenCount,
            @JsonProperty("totalTokenCount") int totalTokenCount
    ) {}
}
