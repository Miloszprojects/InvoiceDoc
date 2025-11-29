package com.softwaremind.invoicedocbackend.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long organizationId;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final boolean approvedByOwner;

    public CustomUserDetails(UserEntity user) {
        this.userId = user.getId();
        this.organizationId = user.getOrganization() != null
                ? user.getOrganization().getId()
                : null;
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.approvedByOwner = user.isApprovedByOwner();
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override public boolean isAccountNonExpired() { return true; }

    @Override public boolean isAccountNonLocked() { return true; }

    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        if (role == UserRole.ADMIN) {
            return true;
        }
        return approvedByOwner;
    }
}
