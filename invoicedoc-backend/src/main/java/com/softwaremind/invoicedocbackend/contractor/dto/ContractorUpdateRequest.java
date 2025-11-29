package com.softwaremind.invoicedocbackend.contractor.dto;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.contractor.ContractorType;

public record ContractorUpdateRequest(
        ContractorType type,
        String name,
        String nip,
        String pesel,
        AddressDto address,
        String email,
        String phone,
        Boolean favorite
) {}
