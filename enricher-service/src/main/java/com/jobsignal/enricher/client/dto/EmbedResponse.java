package com.jobsignal.enricher.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbedResponse(@JsonProperty("embedding") Embedding embedding) {
    public record Embedding(@JsonProperty("values") List<Float> values) {}
}
