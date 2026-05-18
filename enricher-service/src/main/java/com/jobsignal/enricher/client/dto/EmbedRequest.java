package com.jobsignal.enricher.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbedRequest(
        @JsonProperty("model") String model,
        @JsonProperty("content") Content content
) {
    public record Content(@JsonProperty("parts") List<Part> parts) {}
    public record Part(@JsonProperty("text") String text) {}
}
