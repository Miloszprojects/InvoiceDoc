package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.security.dto.AuthRequest;
import com.softwaremind.invoicedocbackend.security.dto.AuthResponse;
import com.softwaremind.invoicedocbackend.security.dto.RegisterRequest;
import com.softwaremind.invoicedocbackend.security.dto.RegisterResponse;
import com.softwaremind.invoicedocbackend.security.jwt.JwtService;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static final String USERNAME = "jdoe";
    private static final String EMAIL = "jdoe@example.com";
    private static final String PASSWORD = "plain-pass";
    private static final String PASSWORD_HASH = "hashed-pass";
    private static final String FULL_NAME = "John Doe";
    private static final String ORG_NAME = "Acme Org";

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
                                  boolean approved,
                                  OrganizationEntity org) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .email(username + "@example.com")
                .passwordHash(passwordHash)
                .fullName("Full " + username)
                .role(role)
                .organization(org)
                .approvedByOwner(approved)
                .build();
    }

    @Test
    @DisplayName("register should throw CONFLICT when email already exists")
    void registerShouldThrowConflictWhenEmailExists() {
        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.OWNER
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new UserEntity()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT),
                () -> assertThat(ex.getReason()).isEqualTo("EMAIL_EXISTS")
        );
    }

    @Test
    @DisplayName("register should throw BAD_REQUEST when organizationName is null or blank")
    void registerShouldThrowBadRequestWhenOrgNameInvalid() {
        RegisterRequest reqNullOrg = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                null,
                UserRole.OWNER
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        ResponseStatusException ex1 = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(reqNullOrg)
        );

        assertAll(
                () -> assertThat(ex1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(ex1.getReason()).isEqualTo("ORG_NAME_REQUIRED")
        );

        RegisterRequest reqBlankOrg = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                "   ",
                UserRole.OWNER
        );

        ResponseStatusException ex2 = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(reqBlankOrg)
        );

        assertAll(
                () -> assertThat(ex2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(ex2.getReason()).isEqualTo("ORG_NAME_REQUIRED")
        );
    }

    @Test
    @DisplayName("register should create new org and approve ADMIN immediately when role is ADMIN")
    void registerShouldCreateOrgAndApproveAdmin() {
        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.ADMIN
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(organizationRepository.findByName(ORG_NAME)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);

        when(organizationRepository.save(any(OrganizationEntity.class)))
                .thenAnswer(invocation -> {
                    OrganizationEntity org = invocation.getArgument(0);
                    org.setId(10L);
                    return org;
                });

        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse resp = authService.register(req);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();

        assertAll(
                () -> assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN),
                () -> assertThat(savedUser.isApprovedByOwner()).isTrue(),
                () -> assertThat(savedUser.getOrganization()).isNotNull(),
                () -> assertThat(savedUser.getOrganization().getName()).isEqualTo(ORG_NAME),
                () -> assertThat(savedUser.getPasswordHash()).isEqualTo(PASSWORD_HASH),

                () -> assertThat(resp.role()).isEqualTo(UserRole.ADMIN),
                () -> assertThat(resp.approvedByOwner()).isTrue(),
                () -> assertThat(resp.organizationId()).isEqualTo(10L)
        );
    }

    @Test
    @DisplayName("register should throw FORBIDDEN ORG_ALREADY_HAS_ADMIN when org already has admin")
    void registerAdminShouldThrowWhenOrgAlreadyHasAdmin() {
        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.ADMIN
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(organizationRepository.findByName(ORG_NAME))
                .thenReturn(Optional.of(createOrg(1L, ORG_NAME)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN),
                () -> assertThat(ex.getReason()).isEqualTo("ORG_ALREADY_HAS_ADMIN")
        );
    }

    @Test
    @DisplayName("register should throw FORBIDDEN ORG_ALREADY_HAS_ADMIN when save of new org breaks unique constraint")
    void registerAdminShouldThrowWhenSavingOrgViolatesConstraint() {
        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.ADMIN
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(organizationRepository.findByName(ORG_NAME)).thenReturn(Optional.empty());
        when(organizationRepository.save(any(OrganizationEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN),
                () -> assertThat(ex.getReason()).isEqualTo("ORG_ALREADY_HAS_ADMIN")
        );
    }

    @Test
    @DisplayName("register should use existing org and not approve OWNER immediately when role is OWNER")
    void registerShouldUseExistingOrgForOwnerAndNotApprove() {
        OrganizationEntity org = createOrg(20L, ORG_NAME);

        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.OWNER
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(organizationRepository.findByName(ORG_NAME)).thenReturn(Optional.of(org));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse resp = authService.register(req);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();

        assertAll(
                () -> assertThat(savedUser.getRole()).isEqualTo(UserRole.OWNER),
                () -> assertThat(savedUser.isApprovedByOwner()).isFalse(),
                () -> assertThat(savedUser.getOrganization()).isSameAs(org),

                () -> assertThat(resp.role()).isEqualTo(UserRole.OWNER),
                () -> assertThat(resp.approvedByOwner()).isFalse(),
                () -> assertThat(resp.organizationId()).isEqualTo(20L)
        );
    }

    @Test
    @DisplayName("register should use existing org and not approve ACCOUNTANT immediately when role is ACCOUNTANT")
    void registerShouldUseExistingOrgForAccountantAndNotApprove() {
        OrganizationEntity org = createOrg(30L, ORG_NAME);

        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.ACCOUNTANT
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(organizationRepository.findByName(ORG_NAME)).thenReturn(Optional.of(org));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse resp = authService.register(req);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());

        UserEntity savedUser = userCaptor.getValue();

        assertAll(
                () -> assertThat(savedUser.getRole()).isEqualTo(UserRole.ACCOUNTANT),
                () -> assertThat(savedUser.isApprovedByOwner()).isFalse(),
                () -> assertThat(savedUser.getOrganization()).isSameAs(org),
                () -> assertThat(resp.role()).isEqualTo(UserRole.ACCOUNTANT),
                () -> assertThat(resp.approvedByOwner()).isFalse(),
                () -> assertThat(resp.organizationId()).isEqualTo(30L)
        );
    }

    @Test
    @DisplayName("register OWNER or ACCOUNTANT should throw BAD_REQUEST ORG_NOT_FOUND when org does not exist")
    void registerOwnerOrAccountantShouldThrowWhenOrgNotFound() {
        RegisterRequest reqOwner = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                UserRole.OWNER
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(organizationRepository.findByName(ORG_NAME)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(reqOwner)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(ex.getReason()).isEqualTo("ORG_NOT_FOUND")
        );
    }

    @Test
    @DisplayName("register should default role to OWNER when req.role is null")
    void registerShouldDefaultRoleToOwnerWhenNull() {
        OrganizationEntity org = createOrg(40L, ORG_NAME);

        RegisterRequest req = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                FULL_NAME,
                ORG_NAME,
                null
        );

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(organizationRepository.findByName(ORG_NAME)).thenReturn(Optional.of(org));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse resp = authService.register(req);

        assertThat(resp.role()).isEqualTo(UserRole.OWNER);
    }

    @Test
    @DisplayName("login should authenticate user and return JWT token")
    void loginShouldAuthenticateAndReturnToken() {
        AuthRequest req = new AuthRequest(USERNAME, PASSWORD);

        OrganizationEntity org = createOrg(1L, ORG_NAME);
        UserEntity user = createUser(100L, USERNAME, PASSWORD_HASH, UserRole.ADMIN, true, org);
        CustomUserDetails cud = new CustomUserDetails(user);

        Authentication authResult = new UsernamePasswordAuthenticationToken(
                cud,
                null,
                cud.getAuthorities()
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authResult);

        String expectedToken = "jwt-token-123";
        when(jwtService.generateToken(USERNAME, 100L, org.getId(), UserRole.ADMIN.name()))
                .thenReturn(expectedToken);

        AuthResponse resp = authService.login(req);

        assertThat(resp.token()).isEqualTo(expectedToken);

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(jwtService).generateToken(USERNAME, 100L, org.getId(), UserRole.ADMIN.name());
    }

    @Test
    @DisplayName("login should map DisabledException to FORBIDDEN ACCOUNT_NOT_APPROVED")
    void loginShouldMapDisabledException() {
        AuthRequest req = new AuthRequest(USERNAME, PASSWORD);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new DisabledException("disabled"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN),
                () -> assertThat(ex.getReason()).isEqualTo("ACCOUNT_NOT_APPROVED")
        );
    }

    @Test
    @DisplayName("login should map BadCredentialsException to UNAUTHORIZED INVALID_CREDENTIALS")
    void loginShouldMapBadCredentialsException() {
        AuthRequest req = new AuthRequest(USERNAME, PASSWORD);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad"));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
                () -> assertThat(ex.getReason()).isEqualTo("INVALID_CREDENTIALS")
        );
    }

    @Test
    @DisplayName("me should return CurrentUser when authentication has CustomUserDetails principal")
    void meShouldReturnCurrentUser() {
        OrganizationEntity org = createOrg(1L, ORG_NAME);
        UserEntity user = createUser(5L, USERNAME, PASSWORD_HASH, UserRole.OWNER, true, org);
        CustomUserDetails cud = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                cud,
                null,
                cud.getAuthorities()
        );

        CurrentUser result = authService.me(auth);

        assertAll(
                () -> assertThat(result.userId()).isEqualTo(5L),
                () -> assertThat(result.organizationId()).isEqualTo(org.getId()),
                () -> assertThat(result.role()).isEqualTo(UserRole.OWNER)
        );
    }

    @Test
    @DisplayName("me should throw UNAUTHORIZED when authentication is null")
    void meShouldThrowUnauthorizedWhenAuthNull() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.me(null)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("me should throw UNAUTHORIZED when principal is not CustomUserDetails")
    void meShouldThrowUnauthorizedWhenPrincipalNotCustomUserDetails() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "some-principal",
                null
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.me(auth)
        );

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
