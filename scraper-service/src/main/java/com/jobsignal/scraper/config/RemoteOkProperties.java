package com.jobsignal.scraper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scraper.remoteok")
public record RemoteOkProperties(
        String baseUrl,
        int timeoutSeconds
) {}