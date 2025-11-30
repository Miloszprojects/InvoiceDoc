package com.softwaremind.invoicedocbackend.security.dto;

import com.softwaremind.invoicedocbackend.security.UserRole;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String fullName,
        String organizationName,
        UserRole role
) {}
