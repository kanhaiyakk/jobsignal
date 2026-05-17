package com.jobsignal.scraper.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobsignal.scraper.model.RawListing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RemoteOkClientImpl implements RemoteOkClient {

    private static final String SOURCE = "remoteok";

    private final WebClient remoteOkWebClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<RawListing> fetchLatestListings() {
        log.info("Fetching listings from RemoteOK API");

        String responseBody = remoteOkWebClient.get()
                .uri("/api")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (responseBody == null) {
            log.warn("RemoteOK API returned null response");
            return List.of();
        }

        return parseListings(responseBody);
    }

    private List<RawListing> parseListings(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!root.isArray()) {
                log.warn("RemoteOK response is not an array");
                return List.of();
            }

            List<RawListing> listings = new ArrayList<>();
            for (JsonNode node : root) {
                if (!node.has("id") || !node.has("position")) {
                    continue;
                }
                parseListing(node).ifPresent(listings::add);
            }

            log.info("Parsed {} listings from RemoteOK", listings.size());
            return listings;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse RemoteOK response: {}", e.getMessage());
            return List.of();
        }
    }

    private Optional<RawListing> parseListing(JsonNode node) {
        try {
            String externalId = node.path("id").asText();
            String title = node.path("position").asText("");
            String company = node.path("company").asText(null);
            String location = node.path("location").asText(null);
            String description = node.path("description").asText(null);
            String applyUrl = node.path("apply_url").asText(node.path("url").asText(null));

            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = node.path("tags");
            if (tagsNode.isArray()) {
                tagsNode.forEach(tag -> tags.add(tag.asText()));
            }

            Instant postedAt = parseDate(node.path("date").asText(null));
            String rawPayload = objectMapper.writeValueAsString(node);

            return Optional.of(new RawListing(
                    externalId, SOURCE, title, company, location,
                    description, applyUrl, postedAt, tags, rawPayload
            ));
        } catch (Exception e) {
            log.warn("Skipping malformed listing node: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Instant parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.debug("Could not parse date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }
}
