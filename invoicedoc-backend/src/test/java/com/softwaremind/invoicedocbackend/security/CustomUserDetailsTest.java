package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class CustomUserDetailsTest {

    private OrganizationEntity createOrg(Long id, String name) {
        return OrganizationEntity.builder()
                .id(id)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserEntity createUser(Long id,
                                  String username,
                                  String passwordHash,
                                  UserRole role,
                                  boolean approvedByOwner,
                                  OrganizationEntity org) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .email(username + "@example.com")
                .passwordHash(passwordHash)
                .fullName("Full " + username)
                .role(role)
                .approvedByOwner(approvedByOwner)
                .organization(org)
                .build();
    }

    @Test
    @DisplayName("constructor should map fields from UserEntity (with organization)")
    void constructorShouldMapFieldsFromUserEntityWithOrganization() {
        // given
        OrganizationEntity org = createOrg(10L, "Acme");
        UserEntity user = createUser(
                1L,
                "jdoe",
                "hashed-pass",
                UserRole.ADMIN,
                true,
                org
        );

        CustomUserDetails cud = new CustomUserDetails(user);

        assertAll(
                () -> assertThat(cud.getUserId()).isEqualTo(1L),
                () -> assertThat(cud.getOrganizationId()).isEqualTo(10L),
                () -> assertThat(cud.getRole()).isEqualTo(UserRole.ADMIN),
                () -> assertThat(cud.getUsername()).isEqualTo("jdoe"),
                () -> assertThat(cud.getPassword()).isEqualTo("hashed-pass")
        );
    }

    @Test
    @DisplayName("constructor should set organizationId to null when user has no organization")
    void constructorShouldSetOrganizationIdNullWhenUserHasNoOrganization() {
        UserEntity user = createUser(
                2L,
                "noorg",
                "hash-noorg",
                UserRole.ACCOUNTANT,
                true,
                null
        );

        CustomUserDetails cud = new CustomUserDetails(user);

        assertAll(
                () -> assertThat(cud.getUserId()).isEqualTo(2L),
                () -> assertThat(cud.getOrganizationId()).isNull(),
                () -> assertThat(cud.getRole()).isEqualTo(UserRole.ACCOUNTANT)
        );
    }

    @Test
    @DisplayName("getAuthorities should return single ROLE_ authority based on user role")
    void getAuthoritiesShouldReturnSingleRoleAuthority() {
        UserEntity user = createUser(
                3L,
                "owner",
                "hash-owner",
                UserRole.OWNER,
                true,
                createOrg(20L, "Org2")
        );

        CustomUserDetails cud = new CustomUserDetails(user);
        Collection<? extends GrantedAuthority> authorities = cud.getAuthorities();

        assertAll(
                () -> assertThat(authorities).hasSize(1),
                () -> assertThat(authorities.iterator().next().getAuthority())
                        .isEqualTo("ROLE_OWNER")
        );
    }

    @Test
    @DisplayName("isEnabled should always return true for ADMIN regardless of approvedByOwner")
    void isEnabledShouldAlwaysBeTrueForAdmin() {
        UserEntity adminNotApproved = createUser(
                4L,
                "admin1",
                "hash-admin1",
                UserRole.ADMIN,
                false,
                createOrg(30L, "OrgAdmin")
        );

        UserEntity adminApproved = createUser(
                5L,
                "admin2",
                "hash-admin2",
                UserRole.ADMIN,
                true,
                createOrg(30L, "OrgAdmin")
        );

        CustomUserDetails cudNotApproved = new CustomUserDetails(adminNotApproved);
        CustomUserDetails cudApproved = new CustomUserDetails(adminApproved);

        assertAll(
                () -> assertThat(cudNotApproved.isEnabled()).isTrue(),
                () -> assertThat(cudApproved.isEnabled()).isTrue()
        );
    }

    @Test
    @DisplayName("isEnabled should depend on approvedByOwner for non-ADMIN roles")
    void isEnabledShouldDependOnApprovedByOwnerForNonAdmin() {
        UserEntity accountantApproved = createUser(
                6L,
                "acc1",
                "hash-acc1",
                UserRole.ACCOUNTANT,
                true,
                createOrg(40L, "OrgAcc")
        );

        UserEntity accountantNotApproved = createUser(
                7L,
                "acc2",
                "hash-acc2",
                UserRole.ACCOUNTANT,
                false,
                createOrg(40L, "OrgAcc")
        );

        UserEntity ownerApproved = createUser(
                8L,
                "owner1",
                "hash-owner1",
                UserRole.OWNER,
                true,
                createOrg(41L, "OrgOwner")
        );

        UserEntity ownerNotApproved = createUser(
                9L,
                "owner2",
                "hash-owner2",
                UserRole.OWNER,
                false,
                createOrg(41L, "OrgOwner")
        );

        CustomUserDetails cudAccApproved = new CustomUserDetails(accountantApproved);
        CustomUserDetails cudAccNotApproved = new CustomUserDetails(accountantNotApproved);
        CustomUserDetails cudOwnerApproved = new CustomUserDetails(ownerApproved);
        CustomUserDetails cudOwnerNotApproved = new CustomUserDetails(ownerNotApproved);

        assertAll(
                () -> assertThat(cudAccApproved.isEnabled()).isTrue(),
                () -> assertThat(cudAccNotApproved.isEnabled()).isFalse(),
                () -> assertThat(cudOwnerApproved.isEnabled()).isTrue(),
                () -> assertThat(cudOwnerNotApproved.isEnabled()).isFalse()
        );
    }

    @Test
    @DisplayName("account non-expired / non-locked / credentials non-expired should always be true")
    void accountFlagsShouldAlwaysBeTrue() {
        UserEntity user = createUser(
                10L,
                "flags",
                "hash-flags",
                UserRole.ACCOUNTANT,
                true,
                createOrg(50L, "OrgFlags")
        );

        CustomUserDetails cud = new CustomUserDetails(user);

        assertAll(
                () -> assertThat(cud.isAccountNonExpired()).isTrue(),
                () -> assertThat(cud.isAccountNonLocked()).isTrue(),
                () -> assertThat(cud.isCredentialsNonExpired()).isTrue()
        );
    }
}
