package com.jobsignal.scraper.contract;

import com.jobsignal.scraper.client.RemoteOkClient;
import com.jobsignal.scraper.model.RawListing;
import com.jobsignal.scraper.persistence.repository.RawListingRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class
ScraperControllerContractIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("scraper.remoteok.base-url", () -> "https://remoteok.com");
        registry.add("scraper.remoteok.timeout-seconds", () -> "10");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private RawListingRepository repository;

    @MockBean
    private RemoteOkClient remoteOkClient;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        repository.deleteAll();
    }

    @Test
    void triggerScrape_returnsContractCompliantResponse() {
        when(remoteOkClient.fetchLatestListings()).thenReturn(List.of(
                new RawListing("ext-s1", "remoteok", "Job Title", "Company",
                        "Remote", "Desc", "https://apply.co", Instant.now(), List.of(), "{}")
        ));

        given()
        .when()
            .post("/api/v1/scraper/trigger")
        .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/scrape-result-response.json"))
            .body("fetched", equalTo(1))
            .body("saved", equalTo(1))
            .body("skipped", equalTo(0));
    }

    @Test
    void triggerScrape_whenCalledTwice_secondCallSkipsExisting() {
        var listing = new RawListing("ext-dup", "remoteok", "Dup Job", "Corp",
                "Remote", "Desc", "https://apply.co", Instant.now(), List.of(), "{}");
        when(remoteOkClient.fetchLatestListings()).thenReturn(List.of(listing));

        given().when().post("/api/v1/scraper/trigger").then().statusCode(200);

        given()
        .when()
            .post("/api/v1/scraper/trigger")
        .then()
            .statusCode(200)
            .body("fetched", equalTo(1))
            .body("saved", equalTo(0))
            .body("skipped", equalTo(1));
    }

    @Test
    void triggerScrape_whenClientReturnsEmpty_returnsZeroCounts() {
        when(remoteOkClient.fetchLatestListings()).thenReturn(List.of());

        given()
        .when()
            .post("/api/v1/scraper/trigger")
        .then()
            .statusCode(200)
            .body("fetched", equalTo(0))
            .body("saved", equalTo(0))
            .body("skipped", equalTo(0));
    }
}
