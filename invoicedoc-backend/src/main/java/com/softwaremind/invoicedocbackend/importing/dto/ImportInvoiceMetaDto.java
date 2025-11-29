package com.softwaremind.invoicedocbackend.importing.dto;

import java.time.LocalDate;

public record ImportInvoiceMetaDto(
        String number,
        LocalDate issueDate,
        LocalDate saleDate,
        LocalDate dueDate,
        String paymentMethod,
        String currency,
        String language
) {}
