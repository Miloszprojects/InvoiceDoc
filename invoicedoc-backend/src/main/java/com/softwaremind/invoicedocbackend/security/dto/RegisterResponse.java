package com.softwaremind.invoicedocbackend.security.dto;

import com.softwaremind.invoicedocbackend.security.UserRole;

public record RegisterResponse(
        Long userId,
        Long organizationId,
        UserRole role,
        boolean approvedByOwner
) {}
