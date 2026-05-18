package com.jobsignal.enricher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsignal.enricher.client.GeminiClient;
import com.jobsignal.enricher.client.GeminiClientImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiClientConfig {

    @Bean
    public GeminiClient geminiClient(WebClient geminiWebClient,
                                     GeminiProperties properties,
                                     MeterRegistry meterRegistry,
                                     ObjectMapper objectMapper) {
        return new GeminiClientImpl(geminiWebClient, properties, meterRegistry, objectMapper);
    }
}
