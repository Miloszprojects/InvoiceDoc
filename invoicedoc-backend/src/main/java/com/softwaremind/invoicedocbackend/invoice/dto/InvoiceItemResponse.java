package com.softwaremind.invoicedocbackend.invoice.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal netUnitPrice,
        String vatRate,
        BigDecimal netTotal,
        BigDecimal vatAmount,
        BigDecimal grossTotal
) {}
