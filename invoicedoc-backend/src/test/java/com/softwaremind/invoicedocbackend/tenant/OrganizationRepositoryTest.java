package com.softwaremind.invoicedocbackend.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrganizationRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("invoicedoc")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        System.out.println(">>> Testcontainers JDBC URL (OrganizationRepositoryTest) = " + postgres.getJdbcUrl());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("app.jwt.secret", () -> "SuperTajnyKluczJWTDoTestowMusibycdlugijakszalony");
        registry.add("app.crypto.secret", () -> "NajwazniejszySekretKryptoDoTestowJestDlugiiBezpieczny");
    }

    @Autowired
    private OrganizationRepository repository;

    private static final String ORG_NAME = "Acme Organization";

    @Test
    @DisplayName("findByName should return organization when it exists")
    void findByNameShouldReturnOrganizationWhenExists() {
        OrganizationEntity entity = OrganizationEntity.builder()
                .name(ORG_NAME)
                .createdAt(LocalDateTime.now())
                .build();

        OrganizationEntity saved = repository.saveAndFlush(entity);

        Optional<OrganizationEntity> found = repository.findByName(ORG_NAME);

        assertAll(
                () -> assertThat(found).isPresent(),
                () -> assertThat(found.get().getId()).isEqualTo(saved.getId()),
                () -> assertThat(found.get().getName()).isEqualTo(ORG_NAME)
        );
    }

    @Test
    @DisplayName("findByName should return empty Optional when organization does not exist")
    void findByNameShouldReturnEmptyWhenNotExists() {
        Optional<OrganizationEntity> found = repository.findByName("Unknown Org");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("saving two organizations with the same name should violate unique constraint")
    void savingTwoOrganizationsWithSameNameShouldFail() {
        repository.deleteAll();
        repository.flush();

        OrganizationEntity org1 = OrganizationEntity.builder()
                .name(ORG_NAME)
                .createdAt(LocalDateTime.now())
                .build();

        OrganizationEntity org2 = OrganizationEntity.builder()
                .name(ORG_NAME)
                .createdAt(LocalDateTime.now().plusMinutes(1))
                .build();

        repository.saveAndFlush(org1);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(org2)
        );
    }
}
