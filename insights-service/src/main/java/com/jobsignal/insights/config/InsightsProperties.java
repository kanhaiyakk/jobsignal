package com.jobsignal.insights.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "insights")
public record InsightsProperties(
        int lookbackDays,
        String reportCron
) {}
