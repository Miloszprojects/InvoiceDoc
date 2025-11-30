package com.softwaremind.invoicedocbackend.importing;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.ContractorRepository;
import com.softwaremind.invoicedocbackend.contractor.ContractorType;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorCreateRequest;
import com.softwaremind.invoicedocbackend.contractor.mapper.ContractorMapper;
import com.softwaremind.invoicedocbackend.importing.dto.*;
import com.softwaremind.invoicedocbackend.invoice.InvoiceService;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.security.CurrentUser;
import com.softwaremind.invoicedocbackend.security.CurrentUserProvider;
import com.softwaremind.invoicedocbackend.security.UserRole;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private SellerProfileRepository sellerProfileRepository;
    @Mock
    private ContractorRepository contractorRepository;
    @Mock
    private ContractorMapper contractorMapper;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private InvoiceImportMapper importMapper;
    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private ImportService importService;

    private static final Long ORG_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final Long SELLER_PROFILE_ID = 100L;
    private static final Long EXISTING_CONTRACTOR_ID = 200L;
    private static final Long NEW_CONTRACTOR_ID = 300L;

    private CurrentUser mockCurrentUser() {
        CurrentUser cu = new CurrentUser(USER_ID, ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);
        return cu;
    }

    private SellerProfileEntity mockSellerProfileInOrg() {
        OrganizationEntity org = OrganizationEntity.builder()
                .id(ORG_ID)
                .name("Org")
                .createdAt(LocalDateTime.now())
                .build();

        AddressEmbeddable addr = AddressEmbeddable.builder()
                .street("Street")
                .buildingNumber("1")
                .apartmentNumber(null)
                .postalCode("00-000")
                .city("City")
                .country("PL")
                .build();

        SellerProfileEntity seller = SellerProfileEntity.builder()
                .id(SELLER_PROFILE_ID)
                .organization(org)
                .name("Seller Sp. z o.o.")
                .nipEncrypted("enc-nip")
                .defaultCurrency("PLN")
                .defaultPaymentTermDays(14)
                .address(addr)
                .build();

        when(sellerProfileRepository.findById(SELLER_PROFILE_ID))
                .thenReturn(Optional.of(seller));

        return seller;
    }

    private InvoiceImportDto sampleImportDto() {
        AddressDto addr = new AddressDto(
                "Street", "1", null, "00-000", "City", "PL"
        );

        ImportPartyDto sellerParty = new ImportPartyDto(
                "COMPANY",
                "Seller Sp. z o.o.",
                "1111111111",
                null,
                addr,
                "PL00123456789000000000000000"
        );

        ImportPartyDto buyerParty = new ImportPartyDto(
                "COMPANY",
                "Buyer Sp. z o.o.",
                "2222222222",
                null,
                addr,
                null
        );

        ImportInvoiceMetaDto meta = new ImportInvoiceMetaDto(
                "INV-001",
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 5),
                LocalDate.of(2024, 2, 10),
                "BANK_TRANSFER",
                "PLN",
                "PL"
        );

        ImportInvoiceItemDto item1 = new ImportInvoiceItemDto(
                "Item 1",
                new BigDecimal("2.00"),
                "pcs",
                new BigDecimal("100.00"),
                "23"
        );

        ImportExtraDto extra = new ImportExtraDto(
                "Some notes",
                true,
                false
        );

        return new InvoiceImportDto(
                sellerParty,
                buyerParty,
                meta,
                List.of(item1),
                extra
        );
    }

    private InvoiceCreateRequest sampleCreateRequest(Long contractorId) {
        InvoiceItemCreateRequest itemReq = new InvoiceItemCreateRequest(
                "Item 1",
                new BigDecimal("2.00"),
                "pcs",
                new BigDecimal("100.00"),
                "23"
        );
        return new InvoiceCreateRequest(
                SELLER_PROFILE_ID,
                contractorId,
                null,
                null,
                null,
                LocalDate.of(2024, 1, 10),
                LocalDate.of(2024, 1, 5),
                LocalDate.of(2024, 2, 10),
                com.softwaremind.invoicedocbackend.invoice.PaymentMethod.BANK_TRANSFER,
                "PLN",
                "Some notes",
                true,
                false,
                List.of(itemReq)
        );
    }

    @Test
    @DisplayName("importFromDto should use existing contractor and create invoice")
    void importFromDtoShouldUseExistingContractorAndCreateInvoice() {
        mockCurrentUser();
        mockSellerProfileInOrg();
        InvoiceImportDto dto = sampleImportDto();

        ContractorEntity existing = ContractorEntity.builder()
                .id(EXISTING_CONTRACTOR_ID)
                .name(dto.buyer().name())
                .type(ContractorType.COMPANY)
                .build();

        when(contractorRepository.findByOrganizationIdAndNameContainingIgnoreCase(
                ORG_ID, dto.buyer().name()
        )).thenReturn(List.of(existing));

        InvoiceCreateRequest createReq = sampleCreateRequest(EXISTING_CONTRACTOR_ID);
        when(importMapper.toCreateRequest(dto, SELLER_PROFILE_ID, EXISTING_CONTRACTOR_ID))
                .thenReturn(createReq);

        InvoiceResponse expectedResp = mock(InvoiceResponse.class);
        when(invoiceService.createInvoice(createReq)).thenReturn(expectedResp);

        InvoiceResponse result = importService.importFromDto(dto, SELLER_PROFILE_ID);

        assertThat(result).isSameAs(expectedResp);

        verify(sellerProfileRepository).findById(SELLER_PROFILE_ID);
        verify(contractorRepository)
                .findByOrganizationIdAndNameContainingIgnoreCase(ORG_ID, dto.buyer().name());
        verify(importMapper).toCreateRequest(dto, SELLER_PROFILE_ID, EXISTING_CONTRACTOR_ID);
        verify(invoiceService).createInvoice(createReq);
        verifyNoMoreInteractions(contractorMapper, organizationRepository, invoiceService,
                importMapper, contractorRepository, sellerProfileRepository);
    }

    @Test
    @DisplayName("importFromDto should create contractor when not found and then create invoice")
    void importFromDtoShouldCreateContractorWhenNotFound() {
        mockCurrentUser();
        mockSellerProfileInOrg();
        InvoiceImportDto dto = sampleImportDto();

        when(contractorRepository.findByOrganizationIdAndNameContainingIgnoreCase(
                ORG_ID, dto.buyer().name()
        )).thenReturn(List.of());

        OrganizationEntity org = OrganizationEntity.builder()
                .id(ORG_ID)
                .name("Org")
                .createdAt(LocalDateTime.now())
                .build();

        when(organizationRepository.findById(ORG_ID))
                .thenReturn(Optional.of(org));

        when(importMapper.mapBuyerType(dto.buyer()))
                .thenReturn(ContractorType.COMPANY);
        when(importMapper.toAddressDto(dto.buyer()))
                .thenReturn(dto.buyer().address());

        ArgumentCaptor<ContractorCreateRequest> cReqCaptor =
                ArgumentCaptor.forClass(ContractorCreateRequest.class);

        ContractorEntity newContractor = ContractorEntity.builder()
                .id(NEW_CONTRACTOR_ID)
                .organization(org)
                .type(ContractorType.COMPANY)
                .name(dto.buyer().name())
                .build();

        when(contractorMapper.fromCreateRequest(any(ContractorCreateRequest.class), eq(org)))
                .thenReturn(newContractor);
        when(contractorRepository.save(newContractor)).thenReturn(newContractor);

        InvoiceCreateRequest createReq = sampleCreateRequest(NEW_CONTRACTOR_ID);
        when(importMapper.toCreateRequest(dto, SELLER_PROFILE_ID, NEW_CONTRACTOR_ID))
                .thenReturn(createReq);

        InvoiceResponse expectedResp = mock(InvoiceResponse.class);
        when(invoiceService.createInvoice(createReq)).thenReturn(expectedResp);

        InvoiceResponse result = importService.importFromDto(dto, SELLER_PROFILE_ID);

        assertThat(result).isSameAs(expectedResp);

        verify(contractorRepository)
                .findByOrganizationIdAndNameContainingIgnoreCase(ORG_ID, dto.buyer().name());
        verify(organizationRepository).findById(ORG_ID);
        verify(importMapper).mapBuyerType(dto.buyer());
        verify(importMapper).toAddressDto(dto.buyer());
        verify(contractorMapper).fromCreateRequest(cReqCaptor.capture(), eq(org));
        verify(contractorRepository).save(newContractor);
        verify(importMapper).toCreateRequest(dto, SELLER_PROFILE_ID, NEW_CONTRACTOR_ID);
        verify(invoiceService).createInvoice(createReq);

        ContractorCreateRequest captured = cReqCaptor.getValue();
        assertAll(
                () -> assertThat(captured.type()).isEqualTo(ContractorType.COMPANY),
                () -> assertThat(captured.name()).isEqualTo(dto.buyer().name()),
                () -> assertThat(captured.nip()).isEqualTo(dto.buyer().nip())
        );
    }

    @Test
    @DisplayName("importFromDto should throw when seller profile not found")
    void importFromDtoShouldThrowWhenSellerProfileNotFound() {
        mockCurrentUser();
        InvoiceImportDto dto = sampleImportDto();

        when(sellerProfileRepository.findById(SELLER_PROFILE_ID))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> importService.importFromDto(dto, SELLER_PROFILE_ID)
        );

        assertThat(ex).hasMessage("Seller profile not found");
        verifyNoInteractions(contractorRepository, invoiceService, importMapper);
    }

    @Test
    @DisplayName("importFromDto should throw when seller profile is in different organization")
    void importFromDtoShouldThrowWhenSellerInDifferentOrg() {
        mockCurrentUser();
        OrganizationEntity otherOrg = OrganizationEntity.builder()
                .id(999L)
                .name("Other")
                .createdAt(LocalDateTime.now())
                .build();

        SellerProfileEntity seller = SellerProfileEntity.builder()
                .id(SELLER_PROFILE_ID)
                .organization(otherOrg)
                .name("Seller")
                .nipEncrypted("enc")
                .defaultCurrency("PLN")
                .defaultPaymentTermDays(14)
                .build();

        when(sellerProfileRepository.findById(SELLER_PROFILE_ID))
                .thenReturn(Optional.of(seller));

        InvoiceImportDto dto = sampleImportDto();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> importService.importFromDto(dto, SELLER_PROFILE_ID)
        );

        assertThat(ex).hasMessage("Seller profile not in your org");
        verifyNoInteractions(contractorRepository, invoiceService, importMapper);
    }

    @Test
    @DisplayName("importFromCsv should parse CSV and delegate to createInvoice pipeline")
    void importFromCsvShouldParseCsvAndCreateInvoice() throws IOException {
        mockCurrentUser();
        mockSellerProfileInOrg();

        String csv = String.join("\n",
                "sellerName,sellerNip,sellerStreet,sellerBuildingNumber,sellerPostalCode,sellerCity,sellerBankAccount," +
                        "buyerName,buyerNip,buyerStreet,buyerBuildingNumber,buyerPostalCode,buyerCity," +
                        "invoiceNumber,issueDate,saleDate,dueDate,paymentMethod,currency,itemDescription," +
                        "itemQuantity,itemUnit,itemNetUnitPrice,itemVatRate,notes,reverseCharge,splitPayment",
                "Seller Sp. z o.o.,1111111111,Street,1,00-000,City,PL00123456789000000000000000," +
                        "Buyer Sp. z o.o.,2222222222,Street,1,00-000,City," +
                        "INV-001,2024-01-10,2024-01-05,2024-02-10,BANK_TRANSFER,PLN," +
                        "Item 1,2.00,pcs,100.00,23,Some notes,true,false",
                "Seller Sp. z o.o.,1111111111,Street,1,00-000,City,PL00123456789000000000000000," +
                        "Buyer Sp. z o.o.,2222222222,Street,1,00-000,City," +
                        "INV-001,2024-01-10,2024-01-05,2024-02-10,BANK_TRANSFER,PLN," +
                        "Item 2,1.00,pcs,50.00,8,Some notes,true,false"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invoices.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        ContractorEntity existing = ContractorEntity.builder()
                .id(EXISTING_CONTRACTOR_ID)
                .name("Buyer Sp. z o.o.")
                .type(ContractorType.COMPANY)
                .build();

        when(contractorRepository.findByOrganizationIdAndNameContainingIgnoreCase(
                ORG_ID, "Buyer Sp. z o.o."
        )).thenReturn(List.of(existing));

        InvoiceCreateRequest createReq = sampleCreateRequest(EXISTING_CONTRACTOR_ID);
        when(importMapper.toCreateRequest(any(InvoiceImportDto.class),
                eq(SELLER_PROFILE_ID),
                eq(EXISTING_CONTRACTOR_ID)))
                .thenReturn(createReq);

        InvoiceResponse expected = mock(InvoiceResponse.class);
        when(invoiceService.createInvoice(createReq)).thenReturn(expected);

        InvoiceResponse result = importService.importFromCsv(file, SELLER_PROFILE_ID);

        assertThat(result).isSameAs(expected);

        verify(contractorRepository)
                .findByOrganizationIdAndNameContainingIgnoreCase(ORG_ID, "Buyer Sp. z o.o.");
        verify(importMapper).toCreateRequest(any(InvoiceImportDto.class),
                eq(SELLER_PROFILE_ID),
                eq(EXISTING_CONTRACTOR_ID));
        verify(invoiceService).createInvoice(createReq);
    }
}
