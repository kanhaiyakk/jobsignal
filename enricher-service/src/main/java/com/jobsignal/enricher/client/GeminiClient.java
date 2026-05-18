package com.jobsignal.enricher.client;

import com.jobsignal.enricher.model.ExtractionResult;

public interface GeminiClient {
    float[] embed(String text);
    ExtractionResult extract(String prompt);
}
