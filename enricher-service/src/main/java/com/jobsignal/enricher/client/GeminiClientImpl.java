package com.jobsignal.enricher.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsignal.enricher.client.dto.EmbedRequest;
import com.jobsignal.enricher.client.dto.EmbedResponse;
import com.jobsignal.enricher.client.dto.GenerateContentRequest;
import com.jobsignal.enricher.client.dto.GenerateContentResponse;
import com.jobsignal.enricher.config.GeminiProperties;
import com.jobsignal.enricher.exception.GeminiException;
import com.jobsignal.enricher.model.ExtractionResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

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
    public float[] embed(String text) {
        EmbedRequest request = new EmbedRequest(
                "models/" + properties.embeddingModel(),
                new EmbedRequest.Content(List.of(new EmbedRequest.Part(text)))
        );

        try {
            EmbedResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:embedContent")
                            .queryParam("key", properties.apiKey())
                            .build(properties.embeddingModel()))
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(EmbedResponse.class)
                    .block();

            if (response == null || response.embedding() == null || response.embedding().values() == null) {
                throw new GeminiException("Empty embedding response from Gemini");
            }

            meterRegistry.counter("gemini.calls", "type", "embed", "status", "success").increment();

            List<Float> values = response.embedding().values();
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;

        } catch (WebClientResponseException e) {
            meterRegistry.counter("gemini.calls", "type", "embed", "status", "error").increment();
            log.error("Gemini embed API error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeminiException("Gemini embed request failed: " + e.getStatusCode(), e);
        }
    }

    @Override
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public ExtractionResult extract(String prompt) {
        GenerateContentRequest request = new GenerateContentRequest(
                List.of(new GenerateContentRequest.Content(
                        List.of(new GenerateContentRequest.Part(prompt))
                )),
                Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", buildExtractionSchema()
                )
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
                throw new GeminiException("Empty extraction response from Gemini");
            }

            String jsonText = response.candidates().get(0).content().parts().get(0).text();

            if (response.usageMetadata() != null) {
                meterRegistry.counter("gemini.tokens.used")
                        .increment(response.usageMetadata().totalTokenCount());
            }
            meterRegistry.counter("gemini.calls", "type", "extract", "status", "success").increment();

            return objectMapper.readValue(jsonText, ExtractionResult.class);

        } catch (WebClientResponseException e) {
            meterRegistry.counter("gemini.calls", "type", "extract", "status", "error").increment();
            log.error("Gemini extract API error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new GeminiException("Gemini extract request failed: " + e.getStatusCode(), e);
        } catch (JsonProcessingException e) {
            throw new GeminiException("Failed to parse Gemini extraction response", e);
        }
    }

    private Map<String, Object> buildExtractionSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "techStack", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "seniority", Map.of("type", "STRING"),
                        "salaryRange", Map.of("type", "STRING"),
                        "experienceYears", Map.of("type", "INTEGER"),
                        "remotePolicy", Map.of("type", "STRING")
                ),
                "required", List.of("techStack", "seniority", "salaryRange", "experienceYears", "remotePolicy")
        );
    }
}
