package com.softwaremind.invoicedocbackend.tenant.dto;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;

public record SellerProfileUpdateRequest(
        String name,
        String nip,
        String regon,
        String krs,
        String bankName,
        String bankAccount,
        AddressDto address,
        String defaultCurrency,
        Integer defaultPaymentTermDays,
        String invoiceNumberPattern
) {}
