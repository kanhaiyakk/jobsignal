package com.jobsignal.enricher.model;

import java.util.List;

public record ExtractionResult(
        List<String> techStack,
        String seniority,
        String salaryRange,
        Integer experienceYears,
        String remotePolicy
) {}
