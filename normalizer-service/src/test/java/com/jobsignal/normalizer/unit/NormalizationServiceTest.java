package com.jobsignal.normalizer.unit;

import com.jobsignal.normalizer.service.NormalizationService;
import com.jobsignal.shared.event.NormalizedListingEvent;
import com.jobsignal.shared.event.RawListingEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationServiceTest {

    private final NormalizationService service = new NormalizationService();

    @Test
    void normalize_tripsWhitespaceFromAllFields() {
        RawListingEvent event = buildEvent("  Senior Engineer  ", "  Acme Corp  ", "  Remote  ");

        NormalizedListingEvent result = service.normalize(event);

        assertThat(result.payload().title()).isEqualTo("Senior Engineer");
        assertThat(result.payload().company()).isEqualTo("Acme Corp");
        assertThat(result.payload().location()).isEqualTo("Remote");
    }

    @Test
    void normalize_stripsHtmlFromDescription() {
        RawListingEvent event = buildEventWithDescription(
                "<p>We are <strong>hiring</strong> a senior engineer.</p>");

        NormalizedListingEvent result = service.normalize(event);

        assertThat(result.payload().descriptionText())
                .isEqualTo("We are hiring a senior engineer.");
    }

    @Test
    void normalize_setsIsRemoteTrue_whenLocationContainsRemote() {
        NormalizedListingEvent result = service.normalize(buildEvent("Title", "Co", "Remote"));
        assertThat(result.payload().isRemote()).isTrue();
    }

    @Test
    void normalize_setsIsRemoteTrue_whenLocationContainsAnywhere() {
        NormalizedListingEvent result = service.normalize(buildEvent("Title", "Co", "Anywhere"));
        assertThat(result.payload().isRemote()).isTrue();
    }

    @Test
    void normalize_setsIsRemoteFalse_whenLocationIsOffice() {
        NormalizedListingEvent result = service.normalize(buildEvent("Title", "Co", "Berlin, Germany"));
        assertThat(result.payload().isRemote()).isFalse();
    }

    @Test
    void normalize_setsIsRemoteFalse_whenLocationIsNull() {
        NormalizedListingEvent result = service.normalize(buildEvent("Title", "Co", null));
        assertThat(result.payload().isRemote()).isFalse();
    }

    @Test
    void normalize_preservesRawListingIdAndEventMetadata() {
        String rawId = UUID.randomUUID().toString();
        RawListingEvent event = buildEventWithId(rawId, "ext-42", "remoteok");

        NormalizedListingEvent result = service.normalize(event);

        assertThat(result.payload().rawListingId()).isEqualTo(rawId);
        assertThat(result.payload().externalId()).isEqualTo("ext-42");
        assertThat(result.payload().source()).isEqualTo("remoteok");
        assertThat(result.eventType()).isEqualTo("LISTING_NORMALIZED");
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.eventId()).isNotNull();
    }

    @Test
    void normalize_handlesNullDescription() {
        RawListingEvent event = buildEventWithDescription(null);
        NormalizedListingEvent result = service.normalize(event);
        assertThat(result.payload().descriptionText()).isNull();
    }

    private RawListingEvent buildEvent(String title, String company, String location) {
        return new RawListingEvent(
                UUID.randomUUID(), "RAW_LISTING_SCRAPED", Instant.now(), 1,
                new RawListingEvent.RawListingPayload(
                        UUID.randomUUID().toString(), "remoteok", "ext-1",
                        title, company, location,
                        "Description text", "https://apply.co", Instant.now(), "{}"
                )
        );
    }

    private RawListingEvent buildEventWithDescription(String description) {
        return new RawListingEvent(
                UUID.randomUUID(), "RAW_LISTING_SCRAPED", Instant.now(), 1,
                new RawListingEvent.RawListingPayload(
                        UUID.randomUUID().toString(), "remoteok", "ext-1",
                        "Title", "Company", "Remote",
                        description, "https://apply.co", Instant.now(), "{}"
                )
        );
    }

    private RawListingEvent buildEventWithId(String id, String externalId, String source) {
        return new RawListingEvent(
                UUID.randomUUID(), "RAW_LISTING_SCRAPED", Instant.now(), 1,
                new RawListingEvent.RawListingPayload(
                        id, source, externalId,
                        "Title", "Company", "Remote",
                        "Description", "https://apply.co", Instant.now(), "{}"
                )
        );
    }
}
