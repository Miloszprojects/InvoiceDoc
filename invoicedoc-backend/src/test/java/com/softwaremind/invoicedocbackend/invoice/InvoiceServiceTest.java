package com.softwaremind.invoicedocbackend.invoice;

import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.ContractorRepository;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.invoice.mapper.InvoiceMapper;
import com.softwaremind.invoicedocbackend.security.CurrentUser;
import com.softwaremind.invoicedocbackend.security.CurrentUserProvider;
import com.softwaremind.invoicedocbackend.security.UserRole;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.softwaremind.invoicedocbackend.invoice.PaymentMethod.BANK_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private InvoiceNumberGeneratorService numberGenerator;

    @Mock
    private InvoiceMapper invoiceMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private InvoiceService invoiceService;

    private static final Long ORG_ID = 10L;
    private static final Long SELLER_PROFILE_ID = 1L;
    private static final Long CONTRACTOR_ID = 2L;
    private static final LocalDate ISSUE_DATE = LocalDate.of(2024, 1, 10);
    private static final LocalDate SALE_DATE = LocalDate.of(2024, 1, 9);
    private static final LocalDate DUE_DATE = LocalDate.of(2024, 1, 20);

    private OrganizationEntity org(Long id) {
        return OrganizationEntity.builder()
                .id(id)
                .name("Org-" + id)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    private SellerProfileEntity sellerProfile(Long orgId) {
        return SellerProfileEntity.builder()
                .id(SELLER_PROFILE_ID)
                .organization(org(orgId))
                .name("Seller")
                .nipEncrypted("enc-nip")
                .defaultCurrency("PLN")
                .defaultPaymentTermDays(14)
                .build();
    }

    private ContractorEntity contractor(Long orgId) {
        return ContractorEntity.builder()
                .id(CONTRACTOR_ID)
                .organization(org(orgId))
                .name("Contractor")
                .favorite(Boolean.FALSE)
                .build();
    }

    private InvoiceCreateRequest createRequest(List<InvoiceItemCreateRequest> items) {
        return new InvoiceCreateRequest(
                SELLER_PROFILE_ID,
                CONTRACTOR_ID,
                null,
                null,
                null,
                ISSUE_DATE,
                SALE_DATE,
                DUE_DATE,
                BANK_TRANSFER,
                "PLN",
                "Notes",
                false,
                false,
                items
        );
    }

    @Test
    @DisplayName("createInvoice should create invoice with correct totals and items")
    void createInvoiceShouldCreateInvoiceWithCorrectTotals() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileEntity sellerProfile = sellerProfile(ORG_ID);
        when(sellerProfileRepository.findById(SELLER_PROFILE_ID))
                .thenReturn(Optional.of(sellerProfile));

        ContractorEntity contractor = contractor(ORG_ID);
        when(contractorRepository.findById(CONTRACTOR_ID))
                .thenReturn(Optional.of(contractor));

        when(numberGenerator.generateNumberFor(sellerProfile, ISSUE_DATE))
                .thenReturn("INV/2024/001");

        InvoiceEntity emptyInvoice = new InvoiceEntity();
        emptyInvoice.setOrganization(sellerProfile.getOrganization());
        emptyInvoice.setItems(new ArrayList<>());
        when(invoiceMapper.createEmptyInvoiceEntity(any(), any(), any(), anyString()))
                .thenReturn(emptyInvoice);

        InvoiceItemCreateRequest item1 = new InvoiceItemCreateRequest(
                "Item 1",
                new BigDecimal("2.00"),
                "pcs",
                new BigDecimal("100.00"),
                "23"
        );
        InvoiceItemCreateRequest item2 = new InvoiceItemCreateRequest(
                "Item 2",
                new BigDecimal("1.00"),
                "pcs",
                new BigDecimal("50.00"),
                "0"
        );

        InvoiceCreateRequest req = createRequest(List.of(item1, item2));

        when(invoiceMapper.toItemEntity(
                any(), any(), any(), any(), any())
        ).thenAnswer(invocation -> {
            InvoiceItemEntity item = new InvoiceItemEntity();
            item.setInvoice(emptyInvoice);
            item.setDescription(((InvoiceItemCreateRequest) invocation.getArgument(0)).description());
            item.setNetTotal(invocation.getArgument(2));
            item.setVatAmount(invocation.getArgument(3));
            item.setGrossTotal(invocation.getArgument(4));
            return item;
        });

        InvoiceEntity savedInvoice = emptyInvoice;
        savedInvoice.setId(123L);
        when(invoiceRepository.save(emptyInvoice)).thenReturn(savedInvoice);

        InvoiceResponse expectedResponse = mock(InvoiceResponse.class);
        when(invoiceMapper.toResponse(savedInvoice)).thenReturn(expectedResponse);

        InvoiceResponse result = invoiceService.createInvoice(req);

        assertThat(result).isSameAs(expectedResponse);

        ArgumentCaptor<InvoiceEntity> invoiceCaptor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        InvoiceEntity captured = invoiceCaptor.getValue();

        assertAll(
                () -> assertThat(captured.getItems()).hasSize(2),
                () -> assertThat(captured.getTotalNet()).isEqualByComparingTo("250.00"),
                () -> assertThat(captured.getTotalVat()).isEqualByComparingTo("46.00"),
                () -> assertThat(captured.getTotalGross()).isEqualByComparingTo("296.00")
        );
    }

    @Test
    @DisplayName("createInvoice should treat invalid VAT rate as zero VAT")
    void createInvoiceShouldTreatInvalidVatAsZero() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileEntity sellerProfile = sellerProfile(ORG_ID);
        when(sellerProfileRepository.findById(SELLER_PROFILE_ID))
                .thenReturn(Optional.of(sellerProfile));

        ContractorEntity contractor = contractor(ORG_ID);
        when(contractorRepository.findById(CONTRACTOR_ID))
                .thenReturn(Optional.of(contractor));

        when(numberGenerator.generateNumberFor(sellerProfile, ISSUE_DATE))
                .thenReturn("INV/2024/002");

        InvoiceEntity emptyInvoice = new InvoiceEntity();
        emptyInvoice.setOrganization(sellerProfile.getOrganization());
        emptyInvoice.setItems(new ArrayList<>());
        when(invoiceMapper.createEmptyInvoiceEntity(any(), any(), any(), anyString()))
                .thenReturn(emptyInvoice);

        InvoiceItemCreateRequest item = new InvoiceItemCreateRequest(
                "Item invalid VAT",
                new BigDecimal("1.00"),
                "pcs",
                new BigDecimal("100.00"),
                "NOT_A_NUMBER"
        );

        InvoiceCreateRequest req = createRequest(List.of(item));

        when(invoiceMapper.toItemEntity(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    InvoiceItemEntity i = new InvoiceItemEntity();
                    i.setInvoice(emptyInvoice);
                    i.setNetTotal(invocation.getArgument(2));
                    i.setVatAmount(invocation.getArgument(3));
                    i.setGrossTotal(invocation.getArgument(4));
                    return i;
                });

        when(invoiceRepository.save(emptyInvoice)).thenReturn(emptyInvoice);
        when(invoiceMapper.toResponse(emptyInvoice)).thenReturn(mock(InvoiceResponse.class));

        invoiceService.createInvoice(req);

        assertAll(
                () -> assertThat(emptyInvoice.getTotalNet()).isEqualByComparingTo("100.00"),
                () -> assertThat(emptyInvoice.getTotalVat()).isEqualByComparingTo("0.00"),
                () -> assertThat(emptyInvoice.getTotalGross()).isEqualByComparingTo("100.00")
        );
    }

    @Test
    @DisplayName("createInvoice should throw when seller profile does not belong to current organization")
    void createInvoiceShouldThrowWhenSellerProfileFromOtherOrg() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileEntity sellerFromOtherOrg = sellerProfile(99L);
        when(sellerProfileRepository.findById(SELLER_PROFILE_ID))
                .thenReturn(Optional.of(sellerFromOtherOrg));

        InvoiceCreateRequest req = createRequest(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> invoiceService.createInvoice(req)
        );

        assertThat(ex).hasMessage("Seller profile does not belong to your organization");
    }

    @Test
    @DisplayName("createInvoice should throw when contractor does not belong to current organization")
    void createInvoiceShouldThrowWhenContractorFromOtherOrg() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileEntity seller = sellerProfile(ORG_ID);
        when(sellerProfileRepository.findById(SELLER_PROFILE_ID))
                .thenReturn(Optional.of(seller));

        ContractorEntity contractorOtherOrg = contractor(99L);
        when(contractorRepository.findById(CONTRACTOR_ID))
                .thenReturn(Optional.of(contractorOtherOrg));

        InvoiceCreateRequest req = createRequest(List.of());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> invoiceService.createInvoice(req)
        );

        assertThat(ex).hasMessage("Contractor does not belong to your organization");
    }

    @Test
    @DisplayName("listInvoices should call findByOrganizationIdAndIssueDateBetween when from & to provided")
    void listInvoicesShouldUseDateRangeWhenFromAndToProvided() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(1L);
        Page<InvoiceEntity> page = new PageImpl<>(List.of(entity));

        when(invoiceRepository.findByOrganizationIdAndIssueDateBetween(
                eq(ORG_ID), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        InvoiceResponse mapped = mock(InvoiceResponse.class);
        when(invoiceMapper.toResponse(entity)).thenReturn(mapped);

        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        Page<InvoiceResponse> result = invoiceService.listInvoices(from, to, 0, 20);

        assertAll(
                () -> assertThat(result.getContent()).hasSize(1),
                () -> assertThat(result.getContent().getFirst()).isSameAs(mapped)
        );

        verify(invoiceRepository).findByOrganizationIdAndIssueDateBetween(
                eq(ORG_ID), eq(from), eq(to), any(Pageable.class));
        verifyNoMoreInteractions(invoiceRepository);
    }

    @Test
    @DisplayName("listInvoices should call findByOrganizationId when from or to is null")
    void listInvoicesShouldUseSimpleQueryWhenDatesMissing() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(1L);
        Page<InvoiceEntity> page = new PageImpl<>(List.of(entity));

        when(invoiceRepository.findByOrganizationId(eq(ORG_ID), any(Pageable.class)))
                .thenReturn(page);

        InvoiceResponse mapped = mock(InvoiceResponse.class);
        when(invoiceMapper.toResponse(entity)).thenReturn(mapped);

        Page<InvoiceResponse> result = invoiceService.listInvoices(null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByOrganizationId(eq(ORG_ID), any(Pageable.class));
        verifyNoMoreInteractions(invoiceRepository);
    }

    @Test
    @DisplayName("getInvoice should return mapped response when user has access")
    void getInvoiceShouldReturnResponseWhenUserHasAccess() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(5L);
        entity.setOrganization(org(ORG_ID));

        when(invoiceRepository.findById(5L)).thenReturn(Optional.of(entity));

        InvoiceResponse mapped = mock(InvoiceResponse.class);
        when(invoiceMapper.toResponse(entity)).thenReturn(mapped);

        InvoiceResponse result = invoiceService.getInvoice(5L);

        assertThat(result).isSameAs(mapped);
    }

    @Test
    @DisplayName("getInvoice should allow ADMIN to access invoice from other organization")
    void getInvoiceShouldAllowAdminForOtherOrganization() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(5L);
        entity.setOrganization(org(99L));

        when(invoiceRepository.findById(5L)).thenReturn(Optional.of(entity));

        InvoiceResponse mapped = mock(InvoiceResponse.class);
        when(invoiceMapper.toResponse(entity)).thenReturn(mapped);

        InvoiceResponse result = invoiceService.getInvoice(5L);

        assertThat(result).isSameAs(mapped);
    }

    @Test
    @DisplayName("getInvoice should throw when user has no access")
    void getInvoiceShouldThrowWhenNoAccess() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.ACCOUNTANT);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(5L);
        entity.setOrganization(org(99L));

        when(invoiceRepository.findById(5L)).thenReturn(Optional.of(entity));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> invoiceService.getInvoice(5L)
        );

        assertThat(ex).hasMessage("No access to this invoice");
    }

    @Test
    @DisplayName("getInvoiceEntityForPdf should return entity when access is allowed")
    void getInvoiceEntityForPdfShouldReturnEntityWhenAccessAllowed() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(7L);
        entity.setOrganization(org(ORG_ID));
        entity.setItems(new ArrayList<>());

        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(entity));

        InvoiceEntity result = invoiceService.getInvoiceEntityForPdf(7L);

        assertThat(result).isSameAs(entity);
    }

    @Test
    @DisplayName("getInvoiceEntityForPdf should throw when user has no access")
    void getInvoiceEntityForPdfShouldThrowWhenNoAccess() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.ACCOUNTANT);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(7L);
        entity.setOrganization(org(99L));
        entity.setItems(new ArrayList<>());

        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(entity));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> invoiceService.getInvoiceEntityForPdf(7L)
        );

        assertThat(ex).hasMessage("No access to this invoice");
    }

    @Test
    @DisplayName("deleteInvoice should delete invoice when user has access")
    void deleteInvoiceShouldDeleteWhenUserHasAccess() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(8L);
        entity.setOrganization(org(ORG_ID));

        when(invoiceRepository.findById(8L)).thenReturn(Optional.of(entity));

        invoiceService.deleteInvoice(8L);

        verify(invoiceRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteInvoice should throw NOT_FOUND when invoice does not exist")
    void deleteInvoiceShouldThrowNotFoundWhenInvoiceMissing() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        when(invoiceRepository.findById(8L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> invoiceService.deleteInvoice(8L)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(ex.getReason()).isEqualTo("INVOICE_NOT_FOUND")
        );
    }

    @Test
    @DisplayName("deleteInvoice should throw FORBIDDEN when invoice belongs to another organization")
    void deleteInvoiceShouldThrowForbiddenWhenNoAccess() {
        CurrentUser cu = new CurrentUser(100L, ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(8L);
        entity.setOrganization(org(99L));

        when(invoiceRepository.findById(8L)).thenReturn(Optional.of(entity));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> invoiceService.deleteInvoice(8L)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN),
                () -> assertThat(ex.getReason()).isEqualTo("NO_ACCESS_TO_INVOICE")
        );
    }
}
