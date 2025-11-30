package com.softwaremind.invoicedocbackend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProviderTest {

    private final CurrentUserProvider provider = new CurrentUserProvider();

    @Mock
    private CustomUserDetails customUserDetails;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUser should return CurrentUser built from CustomUserDetails in SecurityContext")
    void getCurrentUserShouldReturnCurrentUserFromCustomUserDetails() {
        Long userId = 123L;
        Long orgId = 456L;
        UserRole role = UserRole.ADMIN;

        when(customUserDetails.getUserId()).thenReturn(userId);
        when(customUserDetails.getOrganizationId()).thenReturn(orgId);
        when(customUserDetails.getRole()).thenReturn(role);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        CurrentUser result = provider.getCurrentUser();

        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.userId()).isEqualTo(userId),
                () -> assertThat(result.organizationId()).isEqualTo(orgId),
                () -> assertThat(result.role()).isEqualTo(role)
        );
    }

    @Test
    @DisplayName("getCurrentUser should throw IllegalStateException when there is no authentication in SecurityContext")
    void getCurrentUserShouldThrowWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> provider.getCurrentUser()
        );

        assertThat(ex.getMessage()).isEqualTo("No authenticated user in context");
    }

    @Test
    @DisplayName("getCurrentUser should throw IllegalStateException when principal is not CustomUserDetails")
    void getCurrentUserShouldThrowWhenPrincipalIsNotCustomUserDetails() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "some-string-principal",
                null
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> provider.getCurrentUser()
        );

        assertThat(ex.getMessage()).isEqualTo("No authenticated user in context");
    }
}
