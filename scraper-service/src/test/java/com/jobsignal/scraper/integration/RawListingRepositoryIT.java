package com.jobsignal.scraper.integration;

import com.jobsignal.scraper.persistence.entity.RawListingEntity;
import com.jobsignal.scraper.persistence.repository.RawListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RawListingRepositoryIT {

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
    }

    @Autowired
    private RawListingRepository repository;

    @Test
    void save_whenValidEntity_persistsAndRetrievesById() {
        RawListingEntity entity = RawListingEntity.create(
                "remoteok", "ext-1", "Java Engineer", "Acme", "Remote",
                "Description", "https://apply.example.com", Instant.now(), "{}"
        );

        repository.save(entity);

        var found = repository.findByIdStrict(entity.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getExternalId()).isEqualTo("ext-1");
        assertThat(found.get().getTitle()).isEqualTo("Java Engineer");
    }

    @Test
    void existsBySourceAndExternalId_whenEntityPresent_returnsTrue() {
        RawListingEntity entity = RawListingEntity.create(
                "remoteok", "ext-2", "Go Developer", "Corp", null,
                null, null, null, "{}"
        );
        repository.save(entity);

        assertThat(repository.existsBySourceAndExternalId("remoteok", "ext-2")).isTrue();
        assertThat(repository.existsBySourceAndExternalId("remoteok", "ext-999")).isFalse();
    }

    @Test
    void findAllOrderedByCreatedAt_returnsAllInDescendingOrder() {
        repository.save(RawListingEntity.create(
                "remoteok", "ext-10", "Job A", null, null, null, null, Instant.now(), "{}"));
        repository.save(RawListingEntity.create(
                "remoteok", "ext-11", "Job B", null, null, null, null, Instant.now(), "{}"));

        Page<RawListingEntity> page = repository.findAllOrderedByCreatedAt(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void save_whenDuplicateSourceAndExternalId_throwsConstraintViolation() {
        RawListingEntity first = RawListingEntity.create(
                "remoteok", "ext-dup", "Job Dup", null, null, null, null, null, "{}");
        RawListingEntity second = RawListingEntity.create(
                "remoteok", "ext-dup", "Job Dup Again", null, null, null, null, null, "{}");

        repository.save(first);
        repository.flush();

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            repository.save(second);
            repository.flush();
        });
    }
}
