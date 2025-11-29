package com.softwaremind.invoicedocbackend.common.dto;

public record AddressDto(
        String street,
        String buildingNumber,
        String apartmentNumber,
        String postalCode,
        String city,
        String country
) {}
