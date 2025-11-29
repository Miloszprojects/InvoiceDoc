package com.softwaremind.invoicedocbackend.importing.dto;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;

public record ImportPartyDto(
        String type,
        String name,
        String nip,
        String pesel,
        AddressDto address,
        String bankAccount
) {}
