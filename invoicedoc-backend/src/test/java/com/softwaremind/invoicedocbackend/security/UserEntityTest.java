package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class UserEntityTest {

    @Test
    @DisplayName("builder should set all fields correctly")
    void builderShouldSetAllFieldsCorrectly() {
        OrganizationEntity org = OrganizationEntity.builder()
                .id(10L)
                .name("Acme Org")
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .build();

        Long id = 1L;
        String username = "jdoe";
        String email = "jdoe@example.com";
        String passwordHash = "hashed-password";
        String fullName = "John Doe";
        UserRole role = UserRole.ADMIN;
        boolean approvedByOwner = true;

        UserEntity user = UserEntity.builder()
                .id(id)
                .username(username)
                .email(email)
                .passwordHash(passwordHash)
                .fullName(fullName)
                .role(role)
                .organization(org)
                .approvedByOwner(approvedByOwner)
                .build();

        assertAll(
                () -> assertThat(user.getId()).isEqualTo(id),
                () -> assertThat(user.getUsername()).isEqualTo(username),
                () -> assertThat(user.getEmail()).isEqualTo(email),
                () -> assertThat(user.getPasswordHash()).isEqualTo(passwordHash),
                () -> assertThat(user.getFullName()).isEqualTo(fullName),
                () -> assertThat(user.getRole()).isEqualTo(role),
                () -> assertThat(user.getOrganization()).isEqualTo(org),
                () -> assertThat(user.isApprovedByOwner()).isEqualTo(approvedByOwner)
        );
    }

    @Test
    @DisplayName("setters and getters should work correctly")
    void settersAndGettersShouldWorkCorrectly() {
        UserEntity user = new UserEntity();

        OrganizationEntity org = OrganizationEntity.builder()
                .id(20L)
                .name("Other Org")
                .createdAt(LocalDateTime.of(2024, 2, 2, 10, 30))
                .build();

        Long id = 2L;
        String username = "alice";
        String email = "alice@example.com";
        String passwordHash = "hash-2";
        String fullName = "Alice Smith";
        UserRole role = UserRole.OWNER;
        boolean approvedByOwner = false;

        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFullName(fullName);
        user.setRole(role);
        user.setOrganization(org);
        user.setApprovedByOwner(approvedByOwner);

        assertAll(
                () -> assertThat(user.getId()).isEqualTo(id),
                () -> assertThat(user.getUsername()).isEqualTo(username),
                () -> assertThat(user.getEmail()).isEqualTo(email),
                () -> assertThat(user.getPasswordHash()).isEqualTo(passwordHash),
                () -> assertThat(user.getFullName()).isEqualTo(fullName),
                () -> assertThat(user.getRole()).isEqualTo(role),
                () -> assertThat(user.getOrganization()).isEqualTo(org),
                () -> assertThat(user.isApprovedByOwner()).isEqualTo(approvedByOwner)
        );
    }

    @Test
    @DisplayName("no-args constructor should create entity with null refs and false approved flag")
    void noArgsConstructorShouldCreateEntityWithNullFieldsAndFalseApprovedFlag() {
        UserEntity user = new UserEntity();

        assertAll(
                () -> assertThat(user.getId()).isNull(),
                () -> assertThat(user.getUsername()).isNull(),
                () -> assertThat(user.getEmail()).isNull(),
                () -> assertThat(user.getPasswordHash()).isNull(),
                () -> assertThat(user.getFullName()).isNull(),
                () -> assertThat(user.getRole()).isNull(),
                () -> assertThat(user.getOrganization()).isNull(),
                () -> assertThat(user.isApprovedByOwner()).isFalse()
        );
    }
}
