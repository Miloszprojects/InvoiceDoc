package com.softwaremind.invoicedocbackend.security.dto;

public record AuthRequest(
        String username,
        String password
) {}
