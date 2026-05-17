package com.jobsignal.normalizer.service;

import com.jobsignal.shared.event.NormalizedListingEvent;
import com.jobsignal.shared.event.RawListingEvent;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
public class NormalizationService {

    public NormalizedListingEvent normalize(RawListingEvent rawEvent) {
        RawListingEvent.RawListingPayload raw = rawEvent.payload();

        String title = trimmed(raw.title());
        String company = trimmed(raw.company());
        String location = trimmed(raw.location());
        String descriptionText = stripHtml(raw.description());
        String applyUrl = trimmed(raw.applyUrl());
        boolean isRemote = detectRemote(location);

        log.debug("Normalized listing source={} externalId={} isRemote={}",
                raw.source(), raw.externalId(), isRemote);

        return new NormalizedListingEvent(
                UUID.randomUUID(),
                "LISTING_NORMALIZED",
                Instant.now(),
                1,
                new NormalizedListingEvent.NormalizedListingPayload(
                        raw.id(),
                        raw.source(),
                        raw.externalId(),
                        title,
                        company,
                        location,
                        descriptionText,
                        applyUrl,
                        raw.postedAt(),
                        isRemote
                )
        );
    }

    private String trimmed(String value) {
        return value != null ? value.strip() : null;
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        return Jsoup.parse(html).text();
    }

    private boolean detectRemote(String location) {
        if (location == null) {
            return false;
        }
        String lower = location.toLowerCase();
        return lower.contains("remote") || lower.contains("anywhere");
    }
}
