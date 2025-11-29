package com.softwaremind.invoicedocbackend.security;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.softwaremind.invoicedocbackend.security.dto.AuthRequest;
import com.softwaremind.invoicedocbackend.security.dto.AuthResponse;
import com.softwaremind.invoicedocbackend.security.dto.RegisterRequest;
import com.softwaremind.invoicedocbackend.security.dto.RegisterResponse;
import com.softwaremind.invoicedocbackend.security.jwt.JwtService;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest req) {

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "EMAIL_EXISTS");
        }

        if (req.organizationName() == null || req.organizationName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ORG_NAME_REQUIRED");
        }

        String orgName = req.organizationName().trim();
        UserRole role = req.role() != null ? req.role() : UserRole.OWNER;

        OrganizationEntity org;

        switch (role) {
            case ADMIN -> {
                if (organizationRepository.findByName(orgName).isPresent()) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "ORG_ALREADY_HAS_ADMIN"
                    );
                }

                org = OrganizationEntity.builder()
                        .name(orgName)
                        .createdAt(LocalDateTime.now())
                        .build();

                try {
                    organizationRepository.save(org);
                } catch (DataIntegrityViolationException ex) {
                    throw new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "ORG_ALREADY_HAS_ADMIN"
                    );
                }
            }

            case OWNER, ACCOUNTANT -> {
                org = organizationRepository.findByName(orgName)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "ORG_NOT_FOUND"
                        ));
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "ROLE_NOT_SUPPORTED"
            );
        }

        boolean approvedByOwner = (role == UserRole.ADMIN);

        UserEntity user = UserEntity.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName())
                .role(role)
                .organization(org)
                .approvedByOwner(approvedByOwner)
                .build();

        userRepository.save(user);

        return new RegisterResponse(
                user.getId(),
                org.getId(),
                user.getRole(),
                approvedByOwner
        );
    }

    public AuthResponse login(AuthRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );

            CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

            String token = jwtService.generateToken(
                    principal.getUsername(),
                    principal.getUserId(),
                    principal.getOrganizationId(),
                    principal.getRole().name()
            );

            return new AuthResponse(token);

        } catch (DisabledException ex) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_APPROVED"
            );
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS"
            );
        }
    }

    public CurrentUser me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return new CurrentUser(
                principal.getUserId(),
                principal.getOrganizationId(),
                principal.getRole()
        );
    }
}
