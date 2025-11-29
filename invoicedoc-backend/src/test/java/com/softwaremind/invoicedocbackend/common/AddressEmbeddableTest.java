package com.softwaremind.invoicedocbackend.common;

import jakarta.persistence.Embeddable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class AddressEmbeddableTest {

    private static final String STREET = "Test Street";
    private static final String BUILDING_NO = "12A";
    private static final String APARTMENT_NO = "7";
    private static final String POSTAL_CODE = "00-001";
    private static final String CITY = "Warsaw";
    private static final String COUNTRY = "Poland";

    @Test
    @DisplayName("Builder should set all fields correctly")
    void builderShouldSetAllFields() {
        AddressEmbeddable address = AddressEmbeddable.builder()
                .street(STREET)
                .buildingNumber(BUILDING_NO)
                .apartmentNumber(APARTMENT_NO)
                .postalCode(POSTAL_CODE)
                .city(CITY)
                .country(COUNTRY)
                .build();

        assertAll(
                () -> assertThat(address.getStreet()).isEqualTo(STREET),
                () -> assertThat(address.getBuildingNumber()).isEqualTo(BUILDING_NO),
                () -> assertThat(address.getApartmentNumber()).isEqualTo(APARTMENT_NO),
                () -> assertThat(address.getPostalCode()).isEqualTo(POSTAL_CODE),
                () -> assertThat(address.getCity()).isEqualTo(CITY),
                () -> assertThat(address.getCountry()).isEqualTo(COUNTRY)
        );
    }

    @Test
    @DisplayName("No-args constructor and setters should set all fields correctly")
    void settersShouldSetAllFields() {
        AddressEmbeddable address = new AddressEmbeddable();

        address.setStreet(STREET);
        address.setBuildingNumber(BUILDING_NO);
        address.setApartmentNumber(APARTMENT_NO);
        address.setPostalCode(POSTAL_CODE);
        address.setCity(CITY);
        address.setCountry(COUNTRY);

        assertAll(
                () -> assertThat(address.getStreet()).isEqualTo(STREET),
                () -> assertThat(address.getBuildingNumber()).isEqualTo(BUILDING_NO),
                () -> assertThat(address.getApartmentNumber()).isEqualTo(APARTMENT_NO),
                () -> assertThat(address.getPostalCode()).isEqualTo(POSTAL_CODE),
                () -> assertThat(address.getCity()).isEqualTo(CITY),
                () -> assertThat(address.getCountry()).isEqualTo(COUNTRY)
        );
    }

    @Test
    @DisplayName("Class should be annotated as @Embeddable")
    void shouldBeAnnotatedAsEmbeddable() {
        assertThat(AddressEmbeddable.class.isAnnotationPresent(Embeddable.class))
                .isTrue();
    }
}
