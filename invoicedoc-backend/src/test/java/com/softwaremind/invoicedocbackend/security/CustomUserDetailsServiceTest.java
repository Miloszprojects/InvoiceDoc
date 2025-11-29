package com.softwaremind.invoicedocbackend.security;

import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private com.softwaremind.invoicedocbackend.security.CustomUserDetailsService service;

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
    @DisplayName("loadUserByUsername should return CustomUserDetails when user exists")
    void loadUserByUsernameShouldReturnCustomUserDetailsWhenExists() {
        String username = "jdoe";
        UserEntity user = createUser(
                1L,
                username,
                "hashed-pass",
                UserRole.ADMIN,
                true,
                createOrg(10L, "Acme")
        );

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername(username);

        assertAll(
                () -> assertThat(result).isInstanceOf(CustomUserDetails.class),
                () -> assertThat(result.getUsername()).isEqualTo(username),
                () -> assertThat(result.getPassword()).isEqualTo("hashed-pass")
        );

        verify(userRepository).findByUsername(username);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsernameShouldThrowWhenUserNotFound() {
        String username = "unknown";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername(username)
        );

        assertThat(ex.getMessage()).isEqualTo("User %s not found".formatted(username));
        verify(userRepository).findByUsername(username);
        verifyNoMoreInteractions(userRepository);
    }
}
