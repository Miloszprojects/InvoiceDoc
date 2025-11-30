package com.softwaremind.invoicedocbackend.tenant;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class SellerProfileEntityTest {

    private static final Long ID = 1L;
    private static final String NAME = "Seller Sp. z o.o.";
    private static final String NIP_ENCRYPTED = "enc-nip";
    private static final String REGON = "987654321";
    private static final String KRS = "0000123456";
    private static final String BANK_NAME = "Bank Polska";
    private static final String BANK_ACCOUNT = "PL00123456789000000000000000";
    private static final String DEFAULT_CURRENCY = "PLN";
    private static final Integer DEFAULT_TERM_DAYS = 14;
    private static final String LOGO_PATH = "/logos/seller.png";

    private static OrganizationEntity sampleOrganization() {
        return OrganizationEntity.builder()
                .id(10L)
                .name("Acme Organization")
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .build();
    }

    private static AddressEmbeddable sampleAddress() {
        return AddressEmbeddable.builder()
                .street("Test Street")
                .buildingNumber("12A")
                .apartmentNumber("7")
                .postalCode("00-001")
                .city("Warsaw")
                .country("Poland")
                .build();
    }

    @Test
    @DisplayName("builder should set all fields correctly")
    void builderShouldSetAllFields() {
        OrganizationEntity organization = sampleOrganization();
        AddressEmbeddable address = sampleAddress();

        SellerProfileEntity entity = SellerProfileEntity.builder()
                .id(ID)
                .organization(organization)
                .name(NAME)
                .nipEncrypted(NIP_ENCRYPTED)
                .regon(REGON)
                .krs(KRS)
                .bankName(BANK_NAME)
                .bankAccount(BANK_ACCOUNT)
                .address(address)
                .defaultCurrency(DEFAULT_CURRENCY)
                .defaultPaymentTermDays(DEFAULT_TERM_DAYS)
                .logoPath(LOGO_PATH)
                .build();

        assertAll(
                () -> assertThat(entity.getId()).isEqualTo(ID),
                () -> assertThat(entity.getOrganization()).isEqualTo(organization),
                () -> assertThat(entity.getName()).isEqualTo(NAME),
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(NIP_ENCRYPTED),
                () -> assertThat(entity.getRegon()).isEqualTo(REGON),
                () -> assertThat(entity.getKrs()).isEqualTo(KRS),
                () -> assertThat(entity.getBankName()).isEqualTo(BANK_NAME),
                () -> assertThat(entity.getBankAccount()).isEqualTo(BANK_ACCOUNT),
                () -> assertThat(entity.getAddress()).isEqualTo(address),
                () -> assertThat(entity.getDefaultCurrency()).isEqualTo(DEFAULT_CURRENCY),
                () -> assertThat(entity.getDefaultPaymentTermDays()).isEqualTo(DEFAULT_TERM_DAYS),
                () -> assertThat(entity.getLogoPath()).isEqualTo(LOGO_PATH)
        );
    }

    @Test
    @DisplayName("setters and getters should work correctly")
    void settersAndGettersShouldWorkCorrectly() {
        SellerProfileEntity entity = new SellerProfileEntity();
        OrganizationEntity organization = sampleOrganization();
        AddressEmbeddable address = sampleAddress();

        entity.setId(ID);
        entity.setOrganization(organization);
        entity.setName(NAME);
        entity.setNipEncrypted(NIP_ENCRYPTED);
        entity.setRegon(REGON);
        entity.setKrs(KRS);
        entity.setBankName(BANK_NAME);
        entity.setBankAccount(BANK_ACCOUNT);
        entity.setAddress(address);
        entity.setDefaultCurrency(DEFAULT_CURRENCY);
        entity.setDefaultPaymentTermDays(DEFAULT_TERM_DAYS);
        entity.setLogoPath(LOGO_PATH);

        assertAll(
                () -> assertThat(entity.getId()).isEqualTo(ID),
                () -> assertThat(entity.getOrganization()).isEqualTo(organization),
                () -> assertThat(entity.getName()).isEqualTo(NAME),
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(NIP_ENCRYPTED),
                () -> assertThat(entity.getRegon()).isEqualTo(REGON),
                () -> assertThat(entity.getKrs()).isEqualTo(KRS),
                () -> assertThat(entity.getBankName()).isEqualTo(BANK_NAME),
                () -> assertThat(entity.getBankAccount()).isEqualTo(BANK_ACCOUNT),
                () -> assertThat(entity.getAddress()).isEqualTo(address),
                () -> assertThat(entity.getDefaultCurrency()).isEqualTo(DEFAULT_CURRENCY),
                () -> assertThat(entity.getDefaultPaymentTermDays()).isEqualTo(DEFAULT_TERM_DAYS),
                () -> assertThat(entity.getLogoPath()).isEqualTo(LOGO_PATH)
        );
    }

    @Test
    @DisplayName("no-args constructor should create an instance with null fields")
    void noArgsConstructorShouldCreateInstanceWithNullFields() {
        SellerProfileEntity entity = new SellerProfileEntity();

        assertAll(
                () -> assertThat(entity.getId()).isNull(),
                () -> assertThat(entity.getOrganization()).isNull(),
                () -> assertThat(entity.getName()).isNull(),
                () -> assertThat(entity.getNipEncrypted()).isNull(),
                () -> assertThat(entity.getRegon()).isNull(),
                () -> assertThat(entity.getKrs()).isNull(),
                () -> assertThat(entity.getBankName()).isNull(),
                () -> assertThat(entity.getBankAccount()).isNull(),
                () -> assertThat(entity.getAddress()).isNull(),
                () -> assertThat(entity.getDefaultCurrency()).isNull(),
                () -> assertThat(entity.getDefaultPaymentTermDays()).isNull(),
                () -> assertThat(entity.getLogoPath()).isNull()
        );
    }
}
