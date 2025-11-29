package com.softwaremind.invoicedocbackend.importing.dto;

public record ImportExtraDto(
        String notes,
        Boolean reverseCharge,
        Boolean splitPayment
) {}
