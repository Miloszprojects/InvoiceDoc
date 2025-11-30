package com.softwaremind.invoicedocbackend.importing.dto;

import java.math.BigDecimal;

public record ImportInvoiceItemDto(
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal netUnitPrice,
        String vatRate
) {}
