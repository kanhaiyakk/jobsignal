package com.jobsignal.insights.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsignal.insights.client.dto.GenerateContentRequest;
import com.jobsignal.insights.client.dto.GenerateContentResponse;
import com.jobsignal.insights.config.GeminiProperties;
import com.jobsignal.insights.exception.InsightException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Slf4j
public class GeminiClientImpl implements GeminiClient {

    private static final String RESILIENCE_INSTANCE = "gemini";

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public GeminiClientImpl(WebClient geminiWebClient, GeminiProperties properties,
                            MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.geminiWebClient = geminiWebClient;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public String generateReport(String prompt) {
        GenerateContentRequest request = new GenerateContentRequest(
                List.of(new GenerateContentRequest.Content(
                        List.of(new GenerateContentRequest.Part(prompt))
                )),
                null
        );

        try {
            GenerateContentResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", properties.apiKey())
                            .build(properties.chatModel()))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GenerateContentResponse.class)
                    .block();

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new InsightException("Empty response from Gemini");
            }

            if (response.usageMetadata() != null) {
                meterRegistry.counter("gemini.tokens.used").increment(response.usageMetadata().totalTokenCount());
            }
            meterRegistry.counter("gemini.calls", "type", "report", "status", "success").increment();

            return response.candidates().get(0).content().parts().get(0).text();

        } catch (WebClientResponseException e) {
            meterRegistry.counter("gemini.calls", "type", "report", "status", "error").increment();
            log.error("Gemini report API error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new InsightException("Gemini report request failed: " + e.getStatusCode(), e);
        }
    }
}
