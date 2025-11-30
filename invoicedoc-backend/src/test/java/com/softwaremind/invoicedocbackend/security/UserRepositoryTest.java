package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("invoicedoc")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        System.out.println(">>> Testcontainers JDBC URL (UserRepositoryTest) = " + postgres.getJdbcUrl());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("app.jwt.secret", () -> "SuperTajnyKluczJWTDoTestowMusibycdlugijakszalony");
        registry.add("app.crypto.secret", () -> "NajwazniejszySekretKryptoDoTestowJestDlugiiBezpieczny");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity createAndSaveOrganization(String name) {
        OrganizationEntity org = OrganizationEntity.builder()
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
        return organizationRepository.saveAndFlush(org);
    }

    private UserEntity createUser(
            OrganizationEntity org,
            String username,
            String email,
            UserRole role,
            boolean approvedByOwner
    ) {
        return UserEntity.builder()
                .username(username)
                .email(email)
                .passwordHash("hash-" + username)
                .fullName("Full " + username)
                .role(role)
                .organization(org)
                .approvedByOwner(approvedByOwner)
                .build();
    }

    @Test
    @DisplayName("findByUsername should return user when username exists")
    void findByUsernameShouldReturnUserWhenExists() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("OrgOne");
        UserEntity user = userRepository.saveAndFlush(
                createUser(org, "jdoe", "jdoe@example.com", UserRole.ADMIN, true)
        );

        Optional<UserEntity> found = userRepository.findByUsername("jdoe");

        assertAll(
                () -> assertThat(found).isPresent(),
                () -> assertThat(found.get().getId()).isEqualTo(user.getId()),
                () -> assertThat(found.get().getUsername()).isEqualTo("jdoe"),
                () -> assertThat(found.get().getEmail()).isEqualTo("jdoe@example.com")
        );
    }

    @Test
    @DisplayName("findByUsername should return empty Optional when username does not exist")
    void findByUsernameShouldReturnEmptyWhenNotExists() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("OrgOne");
        userRepository.saveAndFlush(
                createUser(org, "existing", "existing@example.com", UserRole.ADMIN, true)
        );

        Optional<UserEntity> found = userRepository.findByUsername("unknown");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByEmail should return user when email exists")
    void findByEmailShouldReturnUserWhenExists() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("OrgEmail");
        UserEntity user = userRepository.saveAndFlush(
                createUser(org, "user1", "user1@example.com", UserRole.OWNER, false)
        );

        Optional<UserEntity> found = userRepository.findByEmail("user1@example.com");

        assertAll(
                () -> assertThat(found).isPresent(),
                () -> assertThat(found.get().getId()).isEqualTo(user.getId()),
                () -> assertThat(found.get().getUsername()).isEqualTo("user1"),
                () -> assertThat(found.get().getEmail()).isEqualTo("user1@example.com")
        );
    }

    @Test
    @DisplayName("findByEmail should return empty Optional when email does not exist")
    void findByEmailShouldReturnEmptyWhenNotExists() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("OrgEmail");
        userRepository.saveAndFlush(
                createUser(org, "user2", "user2@example.com", UserRole.ADMIN, true)
        );

        Optional<UserEntity> found = userRepository.findByEmail("unknown@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAllByOrganizationId should return only users for given organization")
    void findAllByOrganizationIdShouldReturnUsersForGivenOrg() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org1 = createAndSaveOrganization("OrgOne");
        OrganizationEntity org2 = createAndSaveOrganization("OrgTwo");

        UserEntity u1 = userRepository.saveAndFlush(
                createUser(org1, "user1", "user1@org1.com", UserRole.ADMIN, true)
        );
        UserEntity u2 = userRepository.saveAndFlush(
                createUser(org1, "user2", "user2@org1.com", UserRole.OWNER, true)
        );
        userRepository.saveAndFlush(
                createUser(org2, "user3", "user3@org2.com", UserRole.ADMIN, false)
        );

        List<UserEntity> result = userRepository.findAllByOrganizationId(org1.getId());

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result)
                        .extracting(UserEntity::getId)
                        .containsExactlyInAnyOrder(u1.getId(), u2.getId()),
                () -> assertThat(result)
                        .allSatisfy(u -> assertThat(u.getOrganization().getId()).isEqualTo(org1.getId()))
        );
    }

    @Test
    @DisplayName("findAllByOrganizationId should return empty list when organization has no users")
    void findAllByOrganizationIdShouldReturnEmptyWhenNoUsers() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("EmptyOrg");

        List<UserEntity> result = userRepository.findAllByOrganizationId(org.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByOrganizationIdAndRole should return true when user with given role exists in organization")
    void existsByOrganizationIdAndRoleShouldReturnTrueWhenUserWithRoleExists() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("OrgWithAdmin");

        userRepository.saveAndFlush(
                createUser(org, "admin1", "admin1@org.com", UserRole.ADMIN, true)
        );
        userRepository.saveAndFlush(
                createUser(org, "user1", "user1@org.com", UserRole.ACCOUNTANT, true)
        );

        boolean existsAdmin = userRepository.existsByOrganizationIdAndRole(org.getId(), UserRole.ADMIN);
        boolean existsOwner = userRepository.existsByOrganizationIdAndRole(org.getId(), UserRole.OWNER);

        assertAll(
                () -> assertThat(existsAdmin).isTrue(),
                () -> assertThat(existsOwner).isFalse()
        );
    }

    @Test
    @DisplayName("existsByOrganizationIdAndRole should consider organization scope")
    void existsByOrganizationIdAndRoleShouldBeScopedToOrganization() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org1 = createAndSaveOrganization("OrgOne");
        OrganizationEntity org2 = createAndSaveOrganization("OrgTwo");

        userRepository.saveAndFlush(
                createUser(org1, "admin1", "admin1@org1.com", UserRole.ADMIN, true)
        );
        userRepository.saveAndFlush(
                createUser(org2, "admin2", "admin2@org2.com", UserRole.ADMIN, true)
        );

        boolean existsInOrg1 = userRepository.existsByOrganizationIdAndRole(org1.getId(), UserRole.ADMIN);
        boolean existsInOrg2 = userRepository.existsByOrganizationIdAndRole(org2.getId(), UserRole.OWNER);

        assertAll(
                () -> assertThat(existsInOrg1).isTrue(),
                () -> assertThat(existsInOrg2).isFalse()
        );
    }

    @Test
    @DisplayName("existsByOrganizationIdAndRole should return false when organization does not exist")
    void existsByOrganizationIdAndRoleShouldReturnFalseWhenOrganizationNotExist() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        boolean exists = userRepository.existsByOrganizationIdAndRole(9999L, UserRole.ADMIN);

        assertThat(exists).isFalse();
    }
}
