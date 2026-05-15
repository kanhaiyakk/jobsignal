package com.jobsignal.scraper.contract;

import com.jobsignal.scraper.persistence.entity.RawListingEntity;
import com.jobsignal.scraper.persistence.repository.RawListingRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ListingControllerContractIT {

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

    private RawListingEntity savedListing;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        repository.deleteAll();
        savedListing = RawListingEntity.create(
                "remoteok", "ext-contract-1", "Contract Test Job",
                "Test Corp", "Remote", "Job description",
                "https://apply.example.com", Instant.now(), "{\"id\":\"ext-contract-1\"}"
        );
        repository.save(savedListing);
    }

    @Test
    void listListings_returnsContractCompliantPagedResponse() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/v1/listings")
        .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/paged-listing-response.json"))
            .body("totalElements", equalTo(1))
            .body("content[0].source", equalTo("remoteok"));
    }

    @Test
    void getListing_whenExists_returnsContractCompliantResponse() {
        given()
        .when()
            .get("/api/v1/listings/{id}", savedListing.getId())
        .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/listing-response.json"))
            .body("id", equalTo(savedListing.getId().toString()))
            .body("title", equalTo("Contract Test Job"));
    }

    @Test
    void getListing_whenNotFound_returnsContractCompliantError() {
        given()
        .when()
            .get("/api/v1/listings/{id}", UUID.randomUUID())
        .then()
            .statusCode(404)
            .body(matchesJsonSchemaInClasspath("schemas/error-response.json"))
            .body("status", equalTo(404))
            .body("error", equalTo("Not Found"));
    }

    @Test
    void listListings_withDefaultPagination_returns200() {
        given()
        .when()
            .get("/api/v1/listings")
        .then()
            .statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(20));
    }
}
