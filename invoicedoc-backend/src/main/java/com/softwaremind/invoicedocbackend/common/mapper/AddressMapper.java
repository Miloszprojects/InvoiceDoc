package com.softwaremind.invoicedocbackend.common.mapper;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.common.dto.AddressDto;

import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressDto toDto(AddressEmbeddable emb) {
        if (emb == null) return null;
        return new AddressDto(
                emb.getStreet(),
                emb.getBuildingNumber(),
                emb.getApartmentNumber(),
                emb.getPostalCode(),
                emb.getCity(),
                emb.getCountry()
        );
    }

    public AddressEmbeddable toEmbeddable(AddressDto dto) {
        if (dto == null) return null;
        return AddressEmbeddable.builder()
                .street(dto.street())
                .buildingNumber(dto.buildingNumber())
                .apartmentNumber(dto.apartmentNumber())
                .postalCode(dto.postalCode())
                .city(dto.city())
                .country(dto.country())
                .build();
    }
}
