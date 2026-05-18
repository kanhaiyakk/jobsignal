package com.jobsignal.enricher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "enricher")
public record EnricherProperties(
        double deduplicationThreshold,
        long embeddingCacheTtlSeconds,
        long extractionCacheTtlSeconds
) {}
