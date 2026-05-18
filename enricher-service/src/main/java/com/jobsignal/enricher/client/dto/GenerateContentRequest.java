package com.jobsignal.enricher.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record GenerateContentRequest(
        @JsonProperty("contents") List<Content> contents,
        @JsonProperty("generationConfig") Map<String, Object> generationConfig
) {
    public record Content(@JsonProperty("parts") List<Part> parts) {}
    public record Part(@JsonProperty("text") String text) {}
}
