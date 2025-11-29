package com.softwaremind.invoicedocbackend.tenant.mapper;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.common.mapper.AddressMapper;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileCreateRequest;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileResponse;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerProfileMapperTest {

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private SellerProfileMapper mapper;

    private static final String NAME = "Seller Sp. z o.o.";
    private static final String NIP = "1234567890";
    private static final String NIP_ENCRYPTED = "enc-nip";
    private static final String REGON = "987654321";
    private static final String KRS = "0000123456";
    private static final String BANK_NAME = "Bank Polska";
    private static final String BANK_ACCOUNT = "PL00123456789000000000000000";
    private static final String DEFAULT_CURRENCY = "PLN";
    private static final Integer DEFAULT_TERM_DAYS = 14;
    private static final String LOGO_PATH = "/logos/seller.png";
    private static final String INVOICE_NUMBER_PATTERN = "INV-{YYYY}/{NNN}";

    private static final AddressDto ADDRESS_DTO = new AddressDto(
            "Test Street",
            "12A",
            "7",
            "00-001",
            "Warsaw",
            "Poland"
    );

    private static final AddressEmbeddable ADDRESS_EMB = AddressEmbeddable.builder()
            .street("Test Street")
            .buildingNumber("12A")
            .apartmentNumber("7")
            .postalCode("00-001")
            .city("Warsaw")
            .country("Poland")
            .build();

    @Test
    @DisplayName("fromCreateRequest should map all fields and encrypt NIP")
    void fromCreateRequestShouldMapAllFieldsAndEncryptNip() {
        SellerProfileCreateRequest request = new SellerProfileCreateRequest(
                NAME,
                NIP,
                REGON,
                KRS,
                BANK_NAME,
                BANK_ACCOUNT,
                ADDRESS_DTO,
                DEFAULT_CURRENCY,
                DEFAULT_TERM_DAYS
        );

        OrganizationEntity organization = new OrganizationEntity();

        when(cryptoService.encrypt(NIP)).thenReturn(NIP_ENCRYPTED);
        when(addressMapper.toEmbeddable(ADDRESS_DTO)).thenReturn(ADDRESS_EMB);

        SellerProfileEntity result = mapper.fromCreateRequest(request, organization);

        assertAll(
                () -> assertThat(result.getOrganization()).isEqualTo(organization),
                () -> assertThat(result.getName()).isEqualTo(NAME),
                () -> assertThat(result.getNipEncrypted()).isEqualTo(NIP_ENCRYPTED),
                () -> assertThat(result.getRegon()).isEqualTo(REGON),
                () -> assertThat(result.getKrs()).isEqualTo(KRS),
                () -> assertThat(result.getBankName()).isEqualTo(BANK_NAME),
                () -> assertThat(result.getBankAccount()).isEqualTo(BANK_ACCOUNT),
                () -> assertThat(result.getAddress()).isEqualTo(ADDRESS_EMB),
                () -> assertThat(result.getDefaultCurrency()).isEqualTo(DEFAULT_CURRENCY),
                () -> assertThat(result.getDefaultPaymentTermDays()).isEqualTo(DEFAULT_TERM_DAYS)
        );

        verify(cryptoService).encrypt(NIP);
        verify(addressMapper).toEmbeddable(ADDRESS_DTO);
    }

    @Test
    @DisplayName("toResponse should map all fields and decrypt NIP")
    void toResponseShouldMapAllFieldsAndDecryptNip() {
        SellerProfileEntity entity = SellerProfileEntity.builder()
                .id(1L)
                .organization(new OrganizationEntity())
                .name(NAME)
                .nipEncrypted(NIP_ENCRYPTED)
                .regon(REGON)
                .krs(KRS)
                .bankName(BANK_NAME)
                .bankAccount(BANK_ACCOUNT)
                .address(ADDRESS_EMB)
                .defaultCurrency(DEFAULT_CURRENCY)
                .defaultPaymentTermDays(DEFAULT_TERM_DAYS)
                .logoPath(LOGO_PATH)
                .build();

        when(cryptoService.decrypt(NIP_ENCRYPTED)).thenReturn(NIP);
        when(addressMapper.toDto(ADDRESS_EMB)).thenReturn(ADDRESS_DTO);

        SellerProfileResponse response = mapper.toResponse(entity);

        assertAll(
                () -> assertThat(response.id()).isEqualTo(1L),
                () -> assertThat(response.name()).isEqualTo(NAME),
                () -> assertThat(response.nip()).isEqualTo(NIP),
                () -> assertThat(response.regon()).isEqualTo(REGON),
                () -> assertThat(response.krs()).isEqualTo(KRS),
                () -> assertThat(response.bankName()).isEqualTo(BANK_NAME),
                () -> assertThat(response.bankAccount()).isEqualTo(BANK_ACCOUNT),
                () -> assertThat(response.address()).isEqualTo(ADDRESS_DTO),
                () -> assertThat(response.defaultCurrency()).isEqualTo(DEFAULT_CURRENCY),
                () -> assertThat(response.defaultPaymentTermDays()).isEqualTo(DEFAULT_TERM_DAYS),
                () -> assertThat(response.logoPath()).isEqualTo(LOGO_PATH)
        );

        verify(cryptoService).decrypt(NIP_ENCRYPTED);
        verify(addressMapper).toDto(ADDRESS_EMB);
    }

    @Test
    @DisplayName("updateEntity should update all fields and encrypt NIP when provided and non-blank")
    void updateEntityShouldUpdateFieldsAndEncryptNip() {
        SellerProfileEntity entity = SellerProfileEntity.builder()
                .id(1L)
                .name("Old Name")
                .nipEncrypted("old-enc-nip")
                .regon("old-regon")
                .krs("old-krs")
                .bankName("Old Bank")
                .bankAccount("OLD-ACCOUNT")
                .address(ADDRESS_EMB)
                .defaultCurrency("OLD")
                .defaultPaymentTermDays(30)
                .logoPath(LOGO_PATH)
                .build();

        String newNip = "5555555555";
        String newNipEnc = "enc-5555555555";

        AddressDto newAddressDto = new AddressDto(
                "New Street",
                "99B",
                "10",
                "11-222",
                "Gdansk",
                "Poland"
        );
        AddressEmbeddable newAddressEmb = AddressEmbeddable.builder()
                .street("New Street")
                .buildingNumber("99B")
                .apartmentNumber("10")
                .postalCode("11-222")
                .city("Gdansk")
                .country("Poland")
                .build();

        SellerProfileUpdateRequest request = new SellerProfileUpdateRequest(
                NAME + " UPDATED",
                newNip,
                REGON + "X",
                KRS + "Y",
                BANK_NAME + " NEW",
                BANK_ACCOUNT + "NEW",
                newAddressDto,
                DEFAULT_CURRENCY,
                21,
                INVOICE_NUMBER_PATTERN
        );

        when(cryptoService.encrypt(newNip)).thenReturn(newNipEnc);
        when(addressMapper.toEmbeddable(newAddressDto)).thenReturn(newAddressEmb);

        mapper.updateEntity(entity, request);

        assertAll(
                () -> assertThat(entity.getName()).isEqualTo(NAME + " UPDATED"),
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(newNipEnc),
                () -> assertThat(entity.getRegon()).isEqualTo(REGON + "X"),
                () -> assertThat(entity.getKrs()).isEqualTo(KRS + "Y"),
                () -> assertThat(entity.getBankName()).isEqualTo(BANK_NAME + " NEW"),
                () -> assertThat(entity.getBankAccount()).isEqualTo(BANK_ACCOUNT + "NEW"),
                () -> assertThat(entity.getAddress()).isEqualTo(newAddressEmb),
                () -> assertThat(entity.getDefaultCurrency()).isEqualTo(DEFAULT_CURRENCY),
                () -> assertThat(entity.getDefaultPaymentTermDays()).isEqualTo(21)
        );

        verify(cryptoService).encrypt(newNip);
        verify(addressMapper).toEmbeddable(newAddressDto);
    }

    @Test
    @DisplayName("updateEntity should not change NIP when nip is null")
    void updateEntityShouldNotChangeNipWhenNipIsNull() {
        SellerProfileEntity entity = SellerProfileEntity.builder()
                .id(1L)
                .name(NAME)
                .nipEncrypted(NIP_ENCRYPTED)
                .regon(REGON)
                .krs(KRS)
                .bankName(BANK_NAME)
                .bankAccount(BANK_ACCOUNT)
                .address(ADDRESS_EMB)
                .defaultCurrency(DEFAULT_CURRENCY)
                .defaultPaymentTermDays(DEFAULT_TERM_DAYS)
                .logoPath(LOGO_PATH)
                .build();

        SellerProfileUpdateRequest request = new SellerProfileUpdateRequest(
                NAME,
                null,
                REGON,
                KRS,
                BANK_NAME,
                BANK_ACCOUNT,
                ADDRESS_DTO,
                DEFAULT_CURRENCY,
                DEFAULT_TERM_DAYS,
                INVOICE_NUMBER_PATTERN
        );

        when(addressMapper.toEmbeddable(ADDRESS_DTO)).thenReturn(ADDRESS_EMB);

        mapper.updateEntity(entity, request);

        assertAll(
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(NIP_ENCRYPTED),
                () -> assertThat(entity.getName()).isEqualTo(NAME),
                () -> assertThat(entity.getRegon()).isEqualTo(REGON),
                () -> assertThat(entity.getKrs()).isEqualTo(KRS)
        );

        verify(cryptoService, never()).encrypt(anyString());
        verify(addressMapper).toEmbeddable(ADDRESS_DTO);
    }

    @Test
    @DisplayName("updateEntity should not change NIP when nip is blank")
    void updateEntityShouldNotChangeNipWhenNipIsBlank() {
        SellerProfileEntity entity = SellerProfileEntity.builder()
                .id(1L)
                .name(NAME)
                .nipEncrypted(NIP_ENCRYPTED)
                .regon(REGON)
                .krs(KRS)
                .bankName(BANK_NAME)
                .bankAccount(BANK_ACCOUNT)
                .address(ADDRESS_EMB)
                .defaultCurrency(DEFAULT_CURRENCY)
                .defaultPaymentTermDays(DEFAULT_TERM_DAYS)
                .logoPath(LOGO_PATH)
                .build();

        SellerProfileUpdateRequest request = new SellerProfileUpdateRequest(
                NAME,
                "   ",
                REGON,
                KRS,
                BANK_NAME,
                BANK_ACCOUNT,
                ADDRESS_DTO,
                DEFAULT_CURRENCY,
                DEFAULT_TERM_DAYS,
                INVOICE_NUMBER_PATTERN
        );

        when(addressMapper.toEmbeddable(ADDRESS_DTO)).thenReturn(ADDRESS_EMB);

        mapper.updateEntity(entity, request);

        assertAll(
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(NIP_ENCRYPTED),
                () -> assertThat(entity.getName()).isEqualTo(NAME)
        );

        verify(cryptoService, never()).encrypt(anyString());
        verify(addressMapper).toEmbeddable(ADDRESS_DTO);
    }
}
