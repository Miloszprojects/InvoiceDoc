package com.softwaremind.invoicedocbackend.invoice.dto;

import java.time.LocalDate;
import java.util.List;

import com.softwaremind.invoicedocbackend.invoice.PaymentMethod;

public record InvoiceCreateRequest(
        Long sellerProfileId,
        Long contractorId,
        String buyerNameOverride,
        String buyerNipOverride,
        String buyerPeselOverride,
        LocalDate issueDate,
        LocalDate saleDate,
        LocalDate dueDate,
        PaymentMethod paymentMethod,
        String currency,
        String notes,
        Boolean reverseCharge,
        Boolean splitPayment,
        List<InvoiceItemCreateRequest> items
) {}
