package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.security.dto.UserSummaryDto;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    private OrganizationEntity createOrg(Long id, String name) {
        return OrganizationEntity.builder()
                .id(id)
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserEntity createUser(Long id,
                                  String email,
                                  String fullName,
                                  UserRole role,
                                  boolean approved,
                                  OrganizationEntity org) {
        return UserEntity.builder()
                .id(id)
                .username(email.split("@")[0])
                .email(email)
                .passwordHash("hash-" + id)
                .fullName(fullName)
                .role(role)
                .organization(org)
                .approvedByOwner(approved)
                .build();
    }

    private CurrentUser currentUser(long userId, long orgId, UserRole role) {
        return new CurrentUser(userId, orgId, role);
    }

    @Test
    @DisplayName("listUsersForOrg should map all fields to UserSummaryDto")
    void listUsersForOrgShouldMapFields() {
        Long orgId = 10L;
        OrganizationEntity org = createOrg(orgId, "Acme Org");

        UserEntity u1 = createUser(
                1L,
                "admin@example.com",
                "Admin User",
                UserRole.ADMIN,
                true,
                org
        );
        UserEntity u2 = createUser(
                2L,
                "acc@example.com",
                "Accountant User",
                UserRole.ACCOUNTANT,
                false,
                org
        );

        when(userRepository.findAllByOrganizationId(orgId))
                .thenReturn(List.of(u1, u2));

        List<UserSummaryDto> result = service.listUsersForOrg(orgId);

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> {
                    UserSummaryDto s1 = result.get(0);
                    assertAll(
                            () -> assertThat(s1.id()).isEqualTo(1L),
                            () -> assertThat(s1.email()).isEqualTo("admin@example.com"),
                            () -> assertThat(s1.fullName()).isEqualTo("Admin User"),
                            () -> assertThat(s1.role()).isEqualTo(UserRole.ADMIN),
                            () -> assertThat(s1.approvedByOwner()).isTrue()
                    );
                },
                () -> {
                    UserSummaryDto s2 = result.get(1);
                    assertAll(
                            () -> assertThat(s2.id()).isEqualTo(2L),
                            () -> assertThat(s2.email()).isEqualTo("acc@example.com"),
                            () -> assertThat(s2.fullName()).isEqualTo("Accountant User"),
                            () -> assertThat(s2.role()).isEqualTo(UserRole.ACCOUNTANT),
                            () -> assertThat(s2.approvedByOwner()).isFalse()
                    );
                }
        );

        verify(userRepository).findAllByOrganizationId(orgId);
    }

    @Test
    @DisplayName("approveUser should set approvedByOwner=true and save user when currentUser is ADMIN in same org")
    void approveUserShouldSetApprovedTrueWhenAdminInSameOrg() {
        Long orgId = 100L;
        Long targetUserId = 5L;

        OrganizationEntity org = createOrg(orgId, "Org");
        UserEntity target = createUser(
                targetUserId,
                "acc@example.com",
                "Accountant",
                UserRole.ACCOUNTANT,
                false,
                org
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));

        CurrentUser currentUser = currentUser(999L, orgId, UserRole.ADMIN);

        service.approveUser(targetUserId, currentUser);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity saved = captor.getValue();
        assertAll(
                () -> assertThat(saved.getId()).isEqualTo(targetUserId),
                () -> assertThat(saved.isApprovedByOwner()).isTrue()
        );
    }

    @Test
    @DisplayName("approveUser should also allow OWNER to approve user in same org")
    void approveUserShouldAllowOwnerToApprove() {
        Long orgId = 101L;
        Long targetUserId = 6L;

        OrganizationEntity org = createOrg(orgId, "OrgOwner");
        UserEntity target = createUser(
                targetUserId,
                "acc2@example.com",
                "Accountant 2",
                UserRole.ACCOUNTANT,
                false,
                org
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));

        CurrentUser currentUser = currentUser(200L, orgId, UserRole.OWNER);

        service.approveUser(targetUserId, currentUser);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity saved = captor.getValue();
        assertAll(
                () -> assertThat(saved.getId()).isEqualTo(targetUserId),
                () -> assertThat(saved.isApprovedByOwner()).isTrue()
        );
    }

    @Test
    @DisplayName("deactivateUser should set approvedByOwner=false and save user when currentUser is ADMIN in same org")
    void deactivateUserShouldSetApprovedFalseWhenAdminInSameOrg() {
        Long orgId = 200L;
        Long targetUserId = 7L;

        OrganizationEntity org = createOrg(orgId, "Org2");
        UserEntity target = createUser(
                targetUserId,
                "acc3@example.com",
                "Accountant 3",
                UserRole.ACCOUNTANT,
                true,
                org
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));

        CurrentUser currentUser = currentUser(300L, orgId, UserRole.ADMIN);

        service.deactivateUser(targetUserId, currentUser);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity saved = captor.getValue();
        assertAll(
                () -> assertThat(saved.getId()).isEqualTo(targetUserId),
                () -> assertThat(saved.isApprovedByOwner()).isFalse()
        );
    }

    @Test
    @DisplayName("approveUser should throw NOT_FOUND when user does not exist")
    void approveUserShouldThrowNotFoundWhenUserNotExist() {
        Long targetUserId = 10L;
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        CurrentUser currentUser = currentUser(1L, 1L, UserRole.ADMIN);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.approveUser(targetUserId, currentUser)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(404);
        verify(userRepository).findById(targetUserId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveUser should throw FORBIDDEN NO_ACCESS_TO_USER when user belongs to other org")
    void approveUserShouldThrowForbiddenWhenNoAccessToUser() {
        Long targetUserId = 11L;

        OrganizationEntity orgOther = createOrg(300L, "OtherOrg");
        UserEntity target = createUser(
                targetUserId,
                "acc@other.org",
                "Other Org User",
                UserRole.ACCOUNTANT,
                false,
                orgOther
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));

        CurrentUser currentUser = currentUser(2L, 999L, UserRole.ADMIN);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.approveUser(targetUserId, currentUser)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(403),
                () -> assertThat(ex.getReason()).isEqualTo("NO_ACCESS_TO_USER")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveUser should throw BAD_REQUEST ADMIN_ALWAYS_ACTIVE when target user is ADMIN")
    void approveUserShouldThrowAdminAlwaysActiveWhenTargetIsAdmin() {
        Long orgId = 400L;
        Long targetUserId = 12L;

        OrganizationEntity org = createOrg(orgId, "OrgAdmin");
        UserEntity adminUser = createUser(
                targetUserId,
                "admin@org.com",
                "Admin User",
                UserRole.ADMIN,
                true,
                org
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(adminUser));

        CurrentUser currentUser = currentUser(3L, orgId, UserRole.ADMIN);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.approveUser(targetUserId, currentUser)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(400),
                () -> assertThat(ex.getReason()).isEqualTo("ADMIN_ALWAYS_ACTIVE")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveUser should throw BAD_REQUEST CANNOT_CHANGE_SELF_STATUS when current user tries to change self")
    void approveUserShouldThrowCannotChangeSelfStatusWhenTargetIsCurrentUser() {
        Long orgId = 500L;
        Long userId = 13L;

        OrganizationEntity org = createOrg(orgId, "OrgSelf");
        UserEntity user = createUser(
                userId,
                "self@org.com",
                "Self User",
                UserRole.ACCOUNTANT,
                false,
                org
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        CurrentUser currentUser = currentUser(userId, orgId, UserRole.ADMIN);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.approveUser(userId, currentUser)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(400),
                () -> assertThat(ex.getReason()).isEqualTo("CANNOT_CHANGE_SELF_STATUS")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveUser should throw FORBIDDEN ONLY_OWNER_OR_ADMIN when currentUser is ACCOUNTANT")
    void approveUserShouldThrowOnlyOwnerOrAdminWhenAccountant() {
        Long orgId = 600L;
        Long targetUserId = 14L;

        OrganizationEntity org = createOrg(orgId, "OrgRole");
        UserEntity target = createUser(
                targetUserId,
                "acc@org.com",
                "Acc User",
                UserRole.ACCOUNTANT,
                false,
                org
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));

        CurrentUser currentUser = currentUser(4L, orgId, UserRole.ACCOUNTANT);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.approveUser(targetUserId, currentUser)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(403),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_OWNER_OR_ADMIN")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivateUser should throw FORBIDDEN ONLY_ADMIN_CAN_DEACTIVATE when currentUser is OWNER")
    void deactivateUserShouldThrowOnlyAdminCanDeactivateWhenOwner() {
        Long orgId = 700L;
        Long targetUserId = 15L;

        OrganizationEntity org = createOrg(orgId, "OrgDeactivate");
        UserEntity target = createUser(
                targetUserId,
                "acc@org.com",
                "Acc User",
                UserRole.ACCOUNTANT,
                true,
                org
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));

        CurrentUser currentUser = currentUser(5L, orgId, UserRole.OWNER);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.deactivateUser(targetUserId, currentUser)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(403),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_ADMIN_CAN_DEACTIVATE")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivateUser should throw FORBIDDEN ONLY_ADMIN_CAN_DEACTIVATE when currentUser is ACCOUNTANT")
    void deactivateUserShouldThrowOnlyAdminCanDeactivateWhenAccountant() {
        Long orgId = 701L;
        Long targetUserId = 16L;

        OrganizationEntity org = createOrg(orgId, "OrgDeactivate2");
        UserEntity target = createUser(
                targetUserId,
                "acc2@org.com",
                "Acc User 2",
                UserRole.ACCOUNTANT,
                true,
                org
        );

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(target));

        CurrentUser currentUser = currentUser(6L, orgId, UserRole.ACCOUNTANT);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.deactivateUser(targetUserId, currentUser)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(403),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_ADMIN_CAN_DEACTIVATE")
        );
        verify(userRepository, never()).save(any());
    }
}
