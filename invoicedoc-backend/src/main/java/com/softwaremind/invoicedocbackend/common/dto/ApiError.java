package com.softwaremind.invoicedocbackend.common.dto;

public record ApiError(
        String code,
        String message
) {}
