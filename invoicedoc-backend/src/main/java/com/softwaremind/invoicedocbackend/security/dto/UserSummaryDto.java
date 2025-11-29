package com.softwaremind.invoicedocbackend.security.dto;

import com.softwaremind.invoicedocbackend.security.UserRole;

public record UserSummaryDto(
        Long id,
        String email,
        String fullName,
        UserRole role,
        boolean approvedByOwner
) {}
