package com.jobsignal.insights.contract;

import com.jobsignal.insights.client.GeminiClient;
import com.jobsignal.insights.persistence.entity.WeeklyReportEntity;
import com.jobsignal.insights.persistence.repository.ListingSnapshotRepository;
import com.jobsignal.insights.persistence.repository.WeeklyReportRepository;
import com.jobsignal.insights.model.InsightSummary;
import com.jobsignal.insights.model.SkillCount;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class InsightsContractIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @MockBean private GeminiClient geminiClient;
    @MockBean private JobLauncher jobLauncher;
    @MockBean private Job weeklyReportJob;

    @LocalServerPort
    private int port;

    @Autowired private WeeklyReportRepository reportRepository;
    @Autowired private ListingSnapshotRepository snapshotRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        reportRepository.deleteAll();
        snapshotRepository.deleteAll();
    }

    @Test
    void listReports_returnsValidSchema() {
        seedReport();

        given()
                .when()
                .get("/api/v1/reports")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/page-weekly-report-response.json"));
    }

    @Test
    void latestReport_whenExists_returnsValidSchema() {
        seedReport();

        given()
                .when()
                .get("/api/v1/reports/latest")
                .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/weekly-report-response.json"));
    }

    @Test
    void latestReport_whenNoneExist_returns404() {
        given()
                .when()
                .get("/api/v1/reports/latest")
                .then()
                .statusCode(404);
    }

    @Test
    void generateReport_returns202() {
        given()
                .when()
                .post("/api/v1/reports/generate")
                .then()
                .statusCode(202);
    }

    @Test
    void listReports_defaultPaginationParams() {
        given()
                .queryParam("page", 0)
                .queryParam("size", 5)
                .when()
                .get("/api/v1/reports")
                .then()
                .statusCode(200)
                .body("size", equalTo(5));
    }

    private void seedReport() {
        InsightSummary summary = new InsightSummary(
                LocalDate.now().minusDays(7), LocalDate.now(), 100,
                List.of(new SkillCount("Java", 50), new SkillCount("Python", 30)),
                Map.of("SENIOR", 60, "MID", 40),
                Map.of("REMOTE", 70, "ON_SITE", 30),
                List.of("Acme Corp", "Tech Inc"));
        reportRepository.save(WeeklyReportEntity.from(summary, "Strong hiring week in Java."));
    }
}
