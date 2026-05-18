package com.jobsignal.insights.client.dto;

import java.util.List;

public record GenerateContentResponse(
        List<Candidate> candidates,
        UsageMetadata usageMetadata
) {
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
    public record UsageMetadata(int totalTokenCount) {}
}
