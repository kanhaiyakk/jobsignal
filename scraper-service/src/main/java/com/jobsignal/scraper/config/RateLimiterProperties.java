package com.jobsignal.scraper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.rate-limiter")
public record RateLimiterProperties(
        int maxRequestsPerWindow,
        long windowSeconds
) {}
