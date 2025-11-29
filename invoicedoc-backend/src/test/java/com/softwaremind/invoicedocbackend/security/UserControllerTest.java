package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.security.dto.UserSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserController controller;

    @Test
    @DisplayName("listUsers should use current user's organizationId and return list from service")
    void listUsersShouldCallServiceWithCurrentOrgAndReturnList() {
        CurrentUser current = new CurrentUser(1L, 10L, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(current);

        UserSummaryDto u1 = new UserSummaryDto(
                100L,
                "admin@example.com",
                "Admin User",
                UserRole.ADMIN,
                true
        );
        UserSummaryDto u2 = new UserSummaryDto(
                101L,
                "owner@example.com",
                "Owner User",
                UserRole.OWNER,
                false
        );

        when(userService.listUsersForOrg(10L)).thenReturn(List.of(u1, u2));

        List<UserSummaryDto> result = controller.listUsers();

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0)).isSameAs(u1),
                () -> assertThat(result.get(1)).isSameAs(u2)
        );

        verify(currentUserProvider).getCurrentUser();
        verify(userService).listUsersForOrg(10L);
        verifyNoMoreInteractions(userService, currentUserProvider);
    }

    @Test
    @DisplayName("approveUser should pass id and current user to service")
    void approveUserShouldPassIdAndCurrentUserToService() {
        Long targetId = 200L;
        CurrentUser current = new CurrentUser(2L, 20L, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(current);

        controller.approveUser(targetId);

        verify(currentUserProvider).getCurrentUser();
        verify(userService).approveUser(targetId, current);
        verifyNoMoreInteractions(userService, currentUserProvider);
    }

    @Test
    @DisplayName("deactivateUser should pass id and current user to service")
    void deactivateUserShouldPassIdAndCurrentUserToService() {
        Long targetId = 300L;
        CurrentUser current = new CurrentUser(3L, 30L, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(current);

        controller.deactivateUser(targetId);

        verify(currentUserProvider).getCurrentUser();
        verify(userService).deactivateUser(targetId, current);
        verifyNoMoreInteractions(userService, currentUserProvider);
    }
}
