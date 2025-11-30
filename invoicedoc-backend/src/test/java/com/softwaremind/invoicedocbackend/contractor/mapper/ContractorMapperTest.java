package com.softwaremind.invoicedocbackend.contractor.mapper;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.common.mapper.AddressMapper;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.ContractorType;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorCreateRequest;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorResponse;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorUpdateRequest;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
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
class ContractorMapperTest {

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private ContractorMapper mapper;

    private static final ContractorType TYPE = ContractorType.PERSON;
    private static final String NAME = "John Doe";
    private static final String NIP = "1234567890";
    private static final String PESEL = "90010112345";
    private static final String ENCRYPTED_NIP = "enc-nip";
    private static final String ENCRYPTED_PESEL = "enc-pesel";
    private static final String EMAIL = "john.doe@example.com";
    private static final String PHONE = "+48 123 456 789";

    private static final AddressDto ADDRESS_DTO = new AddressDto(
            "Test Street",
            "12A",
            "7",
            "00-001",
            "Warsaw",
            "Poland"
    );

    private static final AddressEmbeddable ADDRESS_EMBEDDABLE = AddressEmbeddable.builder()
            .street("Test Street")
            .buildingNumber("12A")
            .apartmentNumber("7")
            .postalCode("00-001")
            .city("Warsaw")
            .country("Poland")
            .build();


    @Test
    @DisplayName("fromCreateRequest should map all fields and encrypt NIP/PESEL when present")
    void fromCreateRequestShouldMapFieldsAndEncryptSensitiveData() {
        ContractorCreateRequest request = new ContractorCreateRequest(
                TYPE,
                NAME,
                NIP,
                PESEL,
                ADDRESS_DTO,
                EMAIL,
                PHONE,
                true
        );
        OrganizationEntity organization = mock(OrganizationEntity.class);

        when(addressMapper.toEmbeddable(ADDRESS_DTO)).thenReturn(ADDRESS_EMBEDDABLE);
        when(cryptoService.encrypt(NIP)).thenReturn(ENCRYPTED_NIP);
        when(cryptoService.encrypt(PESEL)).thenReturn(ENCRYPTED_PESEL);

        ContractorEntity result = mapper.fromCreateRequest(request, organization);

        assertAll(
                () -> assertThat(result.getOrganization()).isEqualTo(organization),
                () -> assertThat(result.getType()).isEqualTo(TYPE),
                () -> assertThat(result.getName()).isEqualTo(NAME),
                () -> assertThat(result.getNipEncrypted()).isEqualTo(ENCRYPTED_NIP),
                () -> assertThat(result.getPeselEncrypted()).isEqualTo(ENCRYPTED_PESEL),
                () -> assertThat(result.getAddress()).isEqualTo(ADDRESS_EMBEDDABLE),
                () -> assertThat(result.getEmail()).isEqualTo(EMAIL),
                () -> assertThat(result.getPhone()).isEqualTo(PHONE),
                () -> assertThat(result.getFavorite()).isTrue()
        );

        verify(addressMapper).toEmbeddable(ADDRESS_DTO);
        verify(cryptoService).encrypt(NIP);
        verify(cryptoService).encrypt(PESEL);
    }

    @Test
    @DisplayName("fromCreateRequest should not encrypt when NIP/PESEL are null and favorite should default to false")
    void fromCreateRequestShouldHandleNullSensitiveFieldsAndFavorite() {
        ContractorCreateRequest request = new ContractorCreateRequest(
                TYPE,
                NAME,
                null,
                null,
                ADDRESS_DTO,
                EMAIL,
                PHONE,
                null
        );
        OrganizationEntity organization = mock(OrganizationEntity.class);

        when(addressMapper.toEmbeddable(ADDRESS_DTO)).thenReturn(ADDRESS_EMBEDDABLE);

        ContractorEntity result = mapper.fromCreateRequest(request, organization);

        assertAll(
                () -> assertThat(result.getNipEncrypted()).isNull(),
                () -> assertThat(result.getPeselEncrypted()).isNull(),
                () -> assertThat(result.getFavorite()).isFalse()
        );

        verify(cryptoService, never()).encrypt(anyString());
        verify(addressMapper).toEmbeddable(ADDRESS_DTO);
    }

    @Test
    @DisplayName("updateEntity should update all fields and encrypt NIP/PESEL when present")
    void updateEntityShouldUpdateFieldsAndEncryptSensitiveData() {
        AddressDto newAddressDto = new AddressDto(
                "New Street",
                "99B",
                "10",
                "11-222",
                "Krakow",
                "Poland"
        );
        AddressEmbeddable newEmbeddable = AddressEmbeddable.builder()
                .street("New Street")
                .buildingNumber("99B")
                .apartmentNumber("10")
                .postalCode("11-222")
                .city("Krakow")
                .country("Poland")
                .build();

        ContractorEntity entity = ContractorEntity.builder()
                .type(null)
                .name("Old Name")
                .nipEncrypted("old-nip")
                .peselEncrypted("old-pesel")
                .address(ADDRESS_EMBEDDABLE)
                .email("old@example.com")
                .phone("000-000-000")
                .favorite(true)
                .build();

        ContractorUpdateRequest request = new ContractorUpdateRequest(
                TYPE,
                NAME,
                NIP,
                PESEL,
                newAddressDto,
                EMAIL,
                PHONE,
                false
        );

        when(addressMapper.toEmbeddable(newAddressDto)).thenReturn(newEmbeddable);
        when(cryptoService.encrypt(NIP)).thenReturn(ENCRYPTED_NIP);
        when(cryptoService.encrypt(PESEL)).thenReturn(ENCRYPTED_PESEL);

        mapper.updateEntity(entity, request);

        assertAll(
                () -> assertThat(entity.getType()).isEqualTo(TYPE),
                () -> assertThat(entity.getName()).isEqualTo(NAME),
                () -> assertThat(entity.getNipEncrypted()).isEqualTo(ENCRYPTED_NIP),
                () -> assertThat(entity.getPeselEncrypted()).isEqualTo(ENCRYPTED_PESEL),
                () -> assertThat(entity.getAddress()).isEqualTo(newEmbeddable),
                () -> assertThat(entity.getEmail()).isEqualTo(EMAIL),
                () -> assertThat(entity.getPhone()).isEqualTo(PHONE),
                () -> assertThat(entity.getFavorite()).isFalse()
        );

        verify(addressMapper).toEmbeddable(newAddressDto);
        verify(cryptoService).encrypt(NIP);
        verify(cryptoService).encrypt(PESEL);
    }

    @Test
    @DisplayName("updateEntity should clear encrypted fields when NIP/PESEL are null and set favorite to false when null")
    void updateEntityShouldClearSensitiveFieldsWhenNull() {
        ContractorEntity entity = ContractorEntity.builder()
                .type(TYPE)
                .name(NAME)
                .nipEncrypted(ENCRYPTED_NIP)
                .peselEncrypted(ENCRYPTED_PESEL)
                .address(ADDRESS_EMBEDDABLE)
                .email(EMAIL)
                .phone(PHONE)
                .favorite(true)
                .build();

        ContractorUpdateRequest request = new ContractorUpdateRequest(
                TYPE,
                NAME,
                null,
                null,
                ADDRESS_DTO,
                EMAIL,
                PHONE,
                null
        );

        when(addressMapper.toEmbeddable(ADDRESS_DTO)).thenReturn(ADDRESS_EMBEDDABLE);

        mapper.updateEntity(entity, request);

        assertAll(
                () -> assertThat(entity.getNipEncrypted()).isNull(),
                () -> assertThat(entity.getPeselEncrypted()).isNull(),
                () -> assertThat(entity.getFavorite()).isFalse()
        );

        verify(cryptoService, never()).encrypt(anyString());
        verify(addressMapper).toEmbeddable(ADDRESS_DTO);
    }

    @Test
    @DisplayName("toResponse should map all fields and decrypt NIP/PESEL when present")
    void toResponseShouldMapFieldsAndDecryptSensitiveData() {
        ContractorEntity entity = ContractorEntity.builder()
                .id(1L)
                .type(TYPE)
                .name(NAME)
                .nipEncrypted(ENCRYPTED_NIP)
                .peselEncrypted(ENCRYPTED_PESEL)
                .address(ADDRESS_EMBEDDABLE)
                .email(EMAIL)
                .phone(PHONE)
                .favorite(true)
                .build();

        when(addressMapper.toDto(ADDRESS_EMBEDDABLE)).thenReturn(ADDRESS_DTO);
        when(cryptoService.decrypt(ENCRYPTED_NIP)).thenReturn(NIP);
        when(cryptoService.decrypt(ENCRYPTED_PESEL)).thenReturn(PESEL);

        ContractorResponse response = mapper.toResponse(entity);

        assertAll(
                () -> assertThat(response.id()).isEqualTo(1L),
                () -> assertThat(response.type()).isEqualTo(TYPE),
                () -> assertThat(response.name()).isEqualTo(NAME),
                () -> assertThat(response.nip()).isEqualTo(NIP),
                () -> assertThat(response.pesel()).isEqualTo(PESEL),
                () -> assertThat(response.address()).isEqualTo(ADDRESS_DTO),
                () -> assertThat(response.email()).isEqualTo(EMAIL),
                () -> assertThat(response.phone()).isEqualTo(PHONE),
                () -> assertThat(response.favorite()).isTrue()
        );

        verify(addressMapper).toDto(ADDRESS_EMBEDDABLE);
        verify(cryptoService).decrypt(ENCRYPTED_NIP);
        verify(cryptoService).decrypt(ENCRYPTED_PESEL);
    }

    @Test
    @DisplayName("toResponse should handle null encrypted fields and not call decrypt")
    void toResponseShouldHandleNullEncryptedFields() {
        ContractorEntity entity = ContractorEntity.builder()
                .id(2L)
                .type(TYPE)
                .name(NAME)
                .nipEncrypted(null)
                .peselEncrypted(null)
                .address(null)
                .email(EMAIL)
                .phone(PHONE)
                .favorite(false)
                .build();

        when(addressMapper.toDto(null)).thenReturn(null);

        ContractorResponse response = mapper.toResponse(entity);

        assertAll(
                () -> assertThat(response.nip()).isNull(),
                () -> assertThat(response.pesel()).isNull(),
                () -> assertThat(response.address()).isNull(),
                () -> assertThat(response.favorite()).isFalse()
        );

        verify(cryptoService, never()).decrypt(anyString());
        verify(addressMapper).toDto(null);
    }
}
