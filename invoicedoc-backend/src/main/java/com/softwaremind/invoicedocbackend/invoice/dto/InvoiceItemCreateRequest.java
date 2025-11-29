    package com.softwaremind.invoicedocbackend.invoice.dto;

    import java.math.BigDecimal;

    public record InvoiceItemCreateRequest(
            String description,
            BigDecimal quantity,
            String unit,
            BigDecimal netUnitPrice,
            String vatRate
    ) {}
