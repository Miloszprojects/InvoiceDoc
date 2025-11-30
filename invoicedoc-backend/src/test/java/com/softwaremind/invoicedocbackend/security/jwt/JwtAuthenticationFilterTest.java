package com.softwaremind.invoicedocbackend.security.jwt;

import com.softwaremind.invoicedocbackend.security.CustomUserDetails;
import com.softwaremind.invoicedocbackend.security.UserEntity;
import com.softwaremind.invoicedocbackend.security.UserRepository;
import com.softwaremind.invoicedocbackend.security.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Jws<Claims> jws;

    @Mock
    private Claims claims;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ----------------- whitelist paths -----------------

    @Test
    @DisplayName("doFilterInternal should skip JWT processing for /v1/api/auth/login")
    void shouldSkipJwtProcessingForLoginPath() throws Exception {
        when(request.getServletPath()).thenReturn("/v1/api/auth/login");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("doFilterInternal should skip JWT processing for /v1/api/auth/register")
    void shouldSkipJwtProcessingForRegisterPath() throws Exception {
        when(request.getServletPath()).thenReturn("/v1/api/auth/register");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ----------------- missing/invalid header -----------------

    @Test
    @DisplayName("doFilterInternal should skip JWT processing when Authorization header is missing")
    void shouldSkipJwtProcessingWhenAuthorizationHeaderMissing() throws Exception {
        when(request.getServletPath()).thenReturn("/v1/api/other");
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("doFilterInternal should skip JWT processing when Authorization header does not start with Bearer")
    void shouldSkipJwtProcessingWhenAuthorizationHeaderInvalidPrefix() throws Exception {
        when(request.getServletPath()).thenReturn("/v1/api/other");
        when(request.getHeader("Authorization")).thenReturn("Token abc");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService, userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ----------------- valid token -----------------

    @Test
    @DisplayName("doFilterInternal should set Authentication in SecurityContext for valid JWT")
    void shouldSetAuthenticationForValidJwt() throws Exception {
        String token = "valid.jwt.token";
        Long userId = 42L;
        String username = "john.doe";

        when(request.getServletPath()).thenReturn("/v1/api/secure");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // mock JWT parsing
        when(jwtService.parseToken(token)).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.get("uid", Long.class)).thenReturn(userId);
        when(claims.getSubject()).thenReturn(username);

        // mock user
        UserEntity user = UserEntity.builder()
                .id(userId)
                .username(username)
                .email("john.doe@example.com")
                .passwordHash("hash")
                .fullName("John Doe")
                .role(UserRole.ADMIN)
                .organization(null)
                .approvedByOwner(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertAll(
                () -> assertThat(auth).isNotNull(),
                () -> assertThat(auth.getPrincipal()).isInstanceOf(CustomUserDetails.class),
                () -> {
                    CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
                    assertThat(principal.getUsername()).isEqualTo(username);
                }
        );

        verify(jwtService).parseToken(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }

    // ----------------- exceptions: token parsing / user loading -----------------

    @Test
    @DisplayName("doFilterInternal should clear SecurityContext and continue filter chain when JWT parsing fails")
    void shouldClearContextAndContinueWhenJwtParsingFails() throws Exception {
        String token = "invalid.jwt.token";

        when(request.getServletPath()).thenReturn("/v1/api/secure");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        when(jwtService.parseToken(token)).thenThrow(new RuntimeException("Invalid token"));

        // set some pre-existing auth to ensure it's cleared
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtService).parseToken(token);
        verify(userRepository, never()).findById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal should clear SecurityContext and continue when user lookup fails")
    void shouldClearContextAndContinueWhenUserLookupFails() throws Exception {
        String token = "valid.jwt.token";
        Long userId = 99L;

        when(request.getServletPath()).thenReturn("/v1/api/secure");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        when(jwtService.parseToken(token)).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.get("uid", Long.class)).thenReturn(userId);
        when(claims.getSubject()).thenReturn("unknown");

        // repo zwraca empty -> orElseThrow -> wpada w catch(Exception)
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // ustawiamy auth, żeby sprawdzić, że zostanie wyczyszczony
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));

        // when
        filter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtService).parseToken(token);
        verify(userRepository).findById(userId);
        verify(filterChain).doFilter(request, response);
    }
}
