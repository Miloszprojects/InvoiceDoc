package com.softwaremind.invoicedocbackend.invoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.invoice.InvoiceStatus;
import com.softwaremind.invoicedocbackend.invoice.PaymentMethod;
public record InvoiceResponse(
        Long id,
        String number,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate saleDate,
        LocalDate dueDate,
        PaymentMethod paymentMethod,
        String currency,
        String sellerName,
        String sellerNip,
        AddressDto sellerAddress,
        String sellerBankAccount,
        String buyerName,
        String buyerNip,
        AddressDto buyerAddress,
        String notes,
        Boolean reverseCharge,
        Boolean splitPayment,
        BigDecimal totalNet,
        BigDecimal totalVat,
        BigDecimal totalGross,
        List<InvoiceItemResponse> items
) {}
