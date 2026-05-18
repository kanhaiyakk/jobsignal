package com.jobsignal.insights.client.dto;

import java.util.List;

public record GenerateContentRequest(
        List<Content> contents,
        Object generationConfig
) {
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
}
