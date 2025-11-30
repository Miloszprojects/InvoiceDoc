package com.softwaremind.invoicedocbackend.contractor;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class ContractorEntityTest {

    @Test
    @DisplayName("builder should set all fields correctly")
    void builderShouldSetAllFieldsCorrectly() {
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(10L)
                .name("Acme Org")
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .build();

        AddressEmbeddable address = AddressEmbeddable.builder()
                .street("Test Street")
                .buildingNumber("12A")
                .apartmentNumber("7")
                .postalCode("00-001")
                .city("Warsaw")
                .country("Poland")
                .build();

        Long id = 1L;
        ContractorType type = ContractorType.COMPANY;
        String name = "ACME Sp. z o.o.";
        String nipEncrypted = "enc-nip";
        String peselEncrypted = "enc-pesel";
        String email = "test@example.com";
        String phone = "+48 123 456 789";
        Boolean favorite = true;

        ContractorEntity entity = ContractorEntity.builder()
                .id(id)
                .organization(organization)
                .type(type)
                .name(name)
                .nipEncrypted(nipEncrypted)
                .peselEncrypted(peselEncrypted)
                .address(address)
                .email(email)
                .phone(phone)
                .favorite(favorite)
                .build();

        assertAll(
                () -> assertThat(entity.getId()).isEqualTo(id),
                () -> assertThat(entity.getOrganization()).isEqualTo(organization),
                () -> assertThat(entity.getType()).isEqualTo(type),
                () -> assertThat(entity.getName()).isEqualTo(name),
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(nipEncrypted),
                () -> assertThat(entity.getPeselEncrypted()).isEqualTo(peselEncrypted),
                () -> assertThat(entity.getAddress()).isEqualTo(address),
                () -> assertThat(entity.getEmail()).isEqualTo(email),
                () -> assertThat(entity.getPhone()).isEqualTo(phone),
                () -> assertThat(entity.getFavorite()).isEqualTo(favorite)
        );
    }

    @Test
    @DisplayName("setters and getters should work correctly")
    void settersAndGettersShouldWorkCorrectly() {
        ContractorEntity entity = new ContractorEntity();

        OrganizationEntity organization = OrganizationEntity.builder()
                .id(20L)
                .name("Other Org")
                .createdAt(LocalDateTime.of(2024, 2, 2, 10, 30))
                .build();

        AddressEmbeddable address = AddressEmbeddable.builder()
                .street("Another Street")
                .buildingNumber("5B")
                .apartmentNumber("2")
                .postalCode("11-111")
                .city("Krakow")
                .country("Poland")
                .build();

        Long id = 2L;
        ContractorType type = ContractorType.PERSON;
        String name = "John Doe";
        String nipEncrypted = "enc-nip-2";
        String peselEncrypted = "enc-pesel-2";
        String email = "john.doe@example.com";
        String phone = "+48 987 654 321";
        Boolean favorite = false;

        entity.setId(id);
        entity.setOrganization(organization);
        entity.setType(type);
        entity.setName(name);
        entity.setNipEncrypted(nipEncrypted);
        entity.setPeselEncrypted(peselEncrypted);
        entity.setAddress(address);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setFavorite(favorite);

        assertAll(
                () -> assertThat(entity.getId()).isEqualTo(id),
                () -> assertThat(entity.getOrganization()).isEqualTo(organization),
                () -> assertThat(entity.getType()).isEqualTo(type),
                () -> assertThat(entity.getName()).isEqualTo(name),
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(nipEncrypted),
                () -> assertThat(entity.getPeselEncrypted()).isEqualTo(peselEncrypted),
                () -> assertThat(entity.getAddress()).isEqualTo(address),
                () -> assertThat(entity.getEmail()).isEqualTo(email),
                () -> assertThat(entity.getPhone()).isEqualTo(phone),
                () -> assertThat(entity.getFavorite()).isEqualTo(favorite)
        );
    }

    @Test
    @DisplayName("no-args constructor should create entity with null fields")
    void noArgsConstructorShouldCreateEntityWithNullFields() {
        ContractorEntity entity = new ContractorEntity();

        assertAll(
                () -> assertThat(entity.getId()).isNull(),
                () -> assertThat(entity.getOrganization()).isNull(),
                () -> assertThat(entity.getType()).isNull(),
                () -> assertThat(entity.getName()).isNull(),
                () -> assertThat(entity.getNipEncrypted()).isNull(),
                () -> assertThat(entity.getPeselEncrypted()).isNull(),
                () -> assertThat(entity.getAddress()).isNull(),
                () -> assertThat(entity.getEmail()).isNull(),
                () -> assertThat(entity.getPhone()).isNull(),
                () -> assertThat(entity.getFavorite()).isNull()
        );
    }
}
