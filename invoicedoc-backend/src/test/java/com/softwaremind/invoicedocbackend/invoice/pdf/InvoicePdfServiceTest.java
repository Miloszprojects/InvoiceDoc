package com.softwaremind.invoicedocbackend.invoice.pdf;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.invoice.InvoiceEntity;
import com.softwaremind.invoicedocbackend.invoice.InvoiceItemEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.softwaremind.invoicedocbackend.invoice.InvoiceStatus.DRAFT;
import static com.softwaremind.invoicedocbackend.invoice.PaymentMethod.BANK_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private InvoicePdfService pdfService;

    private AddressEmbeddable sampleAddress() {
        return AddressEmbeddable.builder()
                .street("Main St")
                .buildingNumber("10")
                .apartmentNumber("5")
                .postalCode("00-001")
                .city("Warsaw")
                .country("Poland")
                .build();
    }

    private OrganizationEntity sampleOrg() {
        return OrganizationEntity.builder()
                .id(1L)
                .name("Org 1")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private SellerProfileEntity sampleSellerProfile(OrganizationEntity org) {
        return SellerProfileEntity.builder()
                .id(100L)
                .organization(org)
                .name("Seller Sp. z o.o.")
                .nipEncrypted("seller-nip-enc")
                .regon("123456789")
                .krs("0000123456")
                .bankName("Bank Polska")
                .bankAccount("PL00123456789000000000000000")
                .address(sampleAddress())
                .defaultCurrency("PLN")
                .defaultPaymentTermDays(14)
                .build();
    }

    private ContractorEntity sampleContractor(OrganizationEntity org) {
        return ContractorEntity.builder()
                .id(200L)
                .organization(org)
                .type(com.softwaremind.invoicedocbackend.contractor.ContractorType.COMPANY)
                .name("Buyer Sp. z o.o.")
                .nipEncrypted("buyer-nip-enc")
                .peselEncrypted(null)
                .address(sampleAddress())
                .email("buyer@example.com")
                .phone("+48 123 456 789")
                .favorite(false)
                .build();
    }

    private InvoiceItemEntity sampleItem(InvoiceEntity invoice) {
        return InvoiceItemEntity.builder()
                .invoice(invoice)
                .description("Service A")
                .quantity(new BigDecimal("2.00"))
                .unit("pcs")
                .netUnitPrice(new BigDecimal("100.00"))
                .vatRate("23")
                .netTotal(new BigDecimal("200.00"))
                .vatAmount(new BigDecimal("46.00"))
                .grossTotal(new BigDecimal("246.00"))
                .build();
    }

    private InvoiceEntity sampleInvoice() {
        OrganizationEntity org = sampleOrg();
        SellerProfileEntity sellerProfile = sampleSellerProfile(org);
        ContractorEntity contractor = sampleContractor(org);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(500L);
        invoice.setOrganization(org);
        invoice.setSellerProfile(sellerProfile);
        invoice.setContractor(contractor);

        invoice.setSellerName(sellerProfile.getName());
        invoice.setSellerNipEncrypted(sellerProfile.getNipEncrypted());
        invoice.setSellerAddress(sellerProfile.getAddress());
        invoice.setSellerBankAccount(sellerProfile.getBankAccount());

        invoice.setBuyerName(contractor.getName());
        invoice.setBuyerNipEncrypted(contractor.getNipEncrypted());
        invoice.setBuyerAddress(contractor.getAddress());

        invoice.setNumber("FV/2024/01/05/001");
        invoice.setIssueDate(LocalDate.of(2024, 1, 5));
        invoice.setSaleDate(LocalDate.of(2024, 1, 5));
        invoice.setDueDate(LocalDate.of(2024, 1, 20));
        invoice.setPaymentMethod(BANK_TRANSFER);
        invoice.setCurrency("PLN");
        invoice.setStatus(DRAFT);

        invoice.setTotalNet(new BigDecimal("200.00"));
        invoice.setTotalVat(new BigDecimal("46.00"));
        invoice.setTotalGross(new BigDecimal("246.00"));
        invoice.setNotes("Some additional notes for this invoice.");
        invoice.setReverseCharge(false);
        invoice.setSplitPayment(false);

        InvoiceItemEntity item = sampleItem(invoice);
        invoice.setItems(new java.util.ArrayList<>(List.of(item)));

        return invoice;
    }

    @Test
    @DisplayName("generateInvoicePdf should produce non-empty PDF and decrypt seller/buyer NIP")
    void generateInvoicePdfShouldProduceBytesAndUseCryptoService() {
        InvoiceEntity invoice = sampleInvoice();

        when(cryptoService.decrypt("seller-nip-enc")).thenReturn("1234567890");
        when(cryptoService.decrypt("buyer-nip-enc")).thenReturn("9876543210");

        byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);

        assertThat(pdfBytes)
                .isNotNull()
                .isNotEmpty();

        verify(cryptoService).decrypt("seller-nip-enc");
        verify(cryptoService).decrypt("buyer-nip-enc");
        verifyNoMoreInteractions(cryptoService);
    }

    @Test
    @DisplayName("generateInvoicePdf should not call decrypt when encrypted NIPs are null")
    void generateInvoicePdfShouldNotDecryptWhenNipsAreNull() {
        InvoiceEntity invoice = sampleInvoice();
        invoice.setSellerNipEncrypted(null);
        invoice.setBuyerNipEncrypted(null);

        byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);

        assertThat(pdfBytes)
                .isNotNull()
                .isNotEmpty();

        verifyNoInteractions(cryptoService);
    }

    @Test
    @DisplayName("generateInvoicePdf should handle missing optional fields without exception")
    void generateInvoicePdfShouldHandleMissingOptionalData() {
        OrganizationEntity org = sampleOrg();

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(700L);
        invoice.setOrganization(org);
        invoice.setSellerName("Seller Minimal");
        invoice.setSellerNipEncrypted(null);
        invoice.setSellerAddress(null);
        invoice.setSellerBankAccount(null);

        invoice.setBuyerName("Buyer Minimal");
        invoice.setBuyerNipEncrypted(null);
        invoice.setBuyerAddress(null);

        invoice.setNumber("FV/2024/02/01/001");
        invoice.setIssueDate(LocalDate.of(2024, 2, 1));
        invoice.setSaleDate(LocalDate.of(2024, 2, 1));
        invoice.setDueDate(LocalDate.of(2024, 2, 15));
        invoice.setPaymentMethod(BANK_TRANSFER);
        invoice.setCurrency("PLN");
        invoice.setStatus(DRAFT);

        invoice.setTotalNet(BigDecimal.ZERO);
        invoice.setTotalVat(BigDecimal.ZERO);
        invoice.setTotalGross(BigDecimal.ZERO);
        invoice.setReverseCharge(false);
        invoice.setSplitPayment(false);
        invoice.setItems(new java.util.ArrayList<>());

        byte[] pdfBytes = pdfService.generateInvoicePdf(invoice);

        assertThat(pdfBytes)
                .isNotNull()
                .isNotEmpty();

        verifyNoInteractions(cryptoService);
    }
}
