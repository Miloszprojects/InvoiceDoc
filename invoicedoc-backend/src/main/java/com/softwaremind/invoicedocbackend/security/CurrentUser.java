package com.softwaremind.invoicedocbackend.security;

public record CurrentUser(
        Long userId,
        Long organizationId,
        UserRole role
) {}
