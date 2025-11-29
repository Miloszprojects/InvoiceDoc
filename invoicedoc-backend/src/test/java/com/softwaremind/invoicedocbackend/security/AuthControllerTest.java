package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.security.dto.AuthRequest;
import com.softwaremind.invoicedocbackend.security.dto.AuthResponse;
import com.softwaremind.invoicedocbackend.security.dto.RegisterRequest;
import com.softwaremind.invoicedocbackend.security.dto.RegisterResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private static final String USERNAME = "jdoe";
    private static final String EMAIL = "jdoe@example.com";
    private static final String PASSWORD = "secret";
    private static final String FULL_NAME = "John Doe";
    private static final String ORG_NAME = "Acme Org";

    @Test
    @DisplayName("register should delegate to AuthService and return 201 CREATED with body")
    void registerShouldDelegateToServiceAndReturnCreated() {
        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.OWNER
        );

        RegisterResponse serviceResp = new RegisterResponse(
                1L,
                10L,
                UserRole.OWNER,
                false
        );

        when(authService.register(req)).thenReturn(serviceResp);

        ResponseEntity<RegisterResponse> response = controller.register(req);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isEqualTo(serviceResp)
        );

        verify(authService).register(req);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("login should delegate to AuthService and return 200 OK with token")
    void loginShouldDelegateToServiceAndReturnOk() {
        AuthRequest req = new AuthRequest(USERNAME, PASSWORD);
        AuthResponse serviceResp = new AuthResponse("jwt-token-123");

        when(authService.login(req)).thenReturn(serviceResp);

        ResponseEntity<AuthResponse> response = controller.login(req);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isEqualTo(serviceResp)
        );

        verify(authService).login(req);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("me should delegate to AuthService and return CurrentUser")
    void meShouldDelegateToService() {
        Authentication authentication = mock(Authentication.class);

        CurrentUser expected = new CurrentUser(
                5L,
                20L,
                UserRole.ADMIN
        );

        when(authService.me(authentication)).thenReturn(expected);

        CurrentUser result = controller.me(authentication);

        assertThat(result).isEqualTo(expected);

        verify(authService).me(authentication);
        verifyNoMoreInteractions(authService);
    }
}
