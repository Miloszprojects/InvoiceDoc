package com.softwaremind.invoicedocbackend.common.mapper;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.common.dto.AddressDto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class AddressMapperTest {

    private final AddressMapper mapper = new AddressMapper();

    private static final String STREET = "Test Street";
    private static final String BUILDING_NO = "12A";
    private static final String APARTMENT_NO = "7";
    private static final String POSTAL_CODE = "00-001";
    private static final String CITY = "Warsaw";
    private static final String COUNTRY = "Poland";


    @Test
    @DisplayName("toDto should map all fields from AddressEmbeddable to AddressDto")
    void toDtoShouldMapAllFields() {
        AddressEmbeddable embeddable = AddressEmbeddable.builder()
                .street(STREET)
                .buildingNumber(BUILDING_NO)
                .apartmentNumber(APARTMENT_NO)
                .postalCode(POSTAL_CODE)
                .city(CITY)
                .country(COUNTRY)
                .build();


        AddressDto dto = mapper.toDto(embeddable);

        assertAll(
                () -> assertThat(dto).isNotNull(),
                () -> assertThat(dto.street()).isEqualTo(STREET),
                () -> assertThat(dto.buildingNumber()).isEqualTo(BUILDING_NO),
                () -> assertThat(dto.apartmentNumber()).isEqualTo(APARTMENT_NO),
                () -> assertThat(dto.postalCode()).isEqualTo(POSTAL_CODE),
                () -> assertThat(dto.city()).isEqualTo(CITY),
                () -> assertThat(dto.country()).isEqualTo(COUNTRY)
        );
    }

    @Test
    @DisplayName("toDto should return null when input AddressEmbeddable is null")
    void toDtoShouldReturnNullWhenEmbeddableIsNull() {
        AddressDto dto = mapper.toDto(null);

        assertThat(dto).isNull();
    }


    @Test
    @DisplayName("toEmbeddable should map all fields from AddressDto to AddressEmbeddable")
    void toEmbeddableShouldMapAllFields() {
        AddressDto dto = new AddressDto(
                STREET,
                BUILDING_NO,
                APARTMENT_NO,
                POSTAL_CODE,
                CITY,
                COUNTRY
        );

        AddressEmbeddable embeddable = mapper.toEmbeddable(dto);

        assertAll(
                () -> assertThat(embeddable).isNotNull(),
                () -> assertThat(embeddable.getStreet()).isEqualTo(STREET),
                () -> assertThat(embeddable.getBuildingNumber()).isEqualTo(BUILDING_NO),
                () -> assertThat(embeddable.getApartmentNumber()).isEqualTo(APARTMENT_NO),
                () -> assertThat(embeddable.getPostalCode()).isEqualTo(POSTAL_CODE),
                () -> assertThat(embeddable.getCity()).isEqualTo(CITY),
                () -> assertThat(embeddable.getCountry()).isEqualTo(COUNTRY)
        );
    }

    @Test
    @DisplayName("toEmbeddable should return null when input AddressDto is null")
    void toEmbeddableShouldReturnNullWhenDtoIsNull() {
        AddressEmbeddable embeddable = mapper.toEmbeddable(null);

        assertThat(embeddable).isNull();
    }

    @Test
    @DisplayName("round-trip Embeddable -> DTO -> Embeddable should preserve field values")
    void roundTripEmbeddableToDtoToEmbeddableShouldPreserveValues() {
        AddressEmbeddable original = AddressEmbeddable.builder()
                .street(STREET)
                .buildingNumber(BUILDING_NO)
                .apartmentNumber(APARTMENT_NO)
                .postalCode(POSTAL_CODE)
                .city(CITY)
                .country(COUNTRY)
                .build();

        AddressDto dto = mapper.toDto(original);
        AddressEmbeddable mappedBack = mapper.toEmbeddable(dto);

        assertAll(
                () -> assertThat(mappedBack).isNotNull(),
                () -> assertThat(mappedBack).isNotSameAs(original),
                () -> assertThat(mappedBack.getStreet()).isEqualTo(original.getStreet()),
                () -> assertThat(mappedBack.getBuildingNumber()).isEqualTo(original.getBuildingNumber()),
                () -> assertThat(mappedBack.getApartmentNumber()).isEqualTo(original.getApartmentNumber()),
                () -> assertThat(mappedBack.getPostalCode()).isEqualTo(original.getPostalCode()),
                () -> assertThat(mappedBack.getCity()).isEqualTo(original.getCity()),
                () -> assertThat(mappedBack.getCountry()).isEqualTo(original.getCountry())
        );
    }

    @Test
    @DisplayName("round-trip DTO -> Embeddable -> DTO should preserve field values")
    void roundTripDtoToEmbeddableToDtoShouldPreserveValues() {
        AddressDto original = new AddressDto(
                STREET,
                BUILDING_NO,
                APARTMENT_NO,
                POSTAL_CODE,
                CITY,
                COUNTRY
        );

        AddressEmbeddable embeddable = mapper.toEmbeddable(original);
        AddressDto mappedBack = mapper.toDto(embeddable);

        assertAll(
                () -> assertThat(mappedBack).isNotNull(),
                () -> assertThat(mappedBack).isNotSameAs(original),
                () -> assertThat(mappedBack.street()).isEqualTo(original.street()),
                () -> assertThat(mappedBack.buildingNumber()).isEqualTo(original.buildingNumber()),
                () -> assertThat(mappedBack.apartmentNumber()).isEqualTo(original.apartmentNumber()),
                () -> assertThat(mappedBack.postalCode()).isEqualTo(original.postalCode()),
                () -> assertThat(mappedBack.city()).isEqualTo(original.city()),
                () -> assertThat(mappedBack.country()).isEqualTo(original.country())
        );
    }
}
