package com.softwaremind.invoicedocbackend.invoice.mapper;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.common.mapper.AddressMapper;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.invoice.InvoiceEntity;
import com.softwaremind.invoicedocbackend.invoice.InvoiceItemEntity;
import com.softwaremind.invoicedocbackend.invoice.InvoiceStatus;
import com.softwaremind.invoicedocbackend.invoice.PaymentMethod;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemResponse;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceMapperTest {

    @Mock
    private AddressMapper addressMapper;

    @Mock
    private CryptoService cryptoService;

    @InjectMocks
    private InvoiceMapper mapper;

    private static final Long SELLER_PROFILE_ID = 10L;
    private static final Long CONTRACTOR_ID = 20L;
    private static final String GENERATED_NUMBER = "INV-2024/001";

    private static final LocalDate ISSUE_DATE = LocalDate.of(2024, 1, 10);
    private static final LocalDate SALE_DATE = LocalDate.of(2024, 1, 5);
    private static final LocalDate DUE_DATE = LocalDate.of(2024, 2, 10);

    private static final String SELLER_NAME = "Seller Sp. z o.o.";
    private static final String SELLER_NIP_ENC = "seller-enc-nip";
    private static final String SELLER_NIP_PLAIN = "1111111111";
    private static final String SELLER_BANK_ACCOUNT = "PL00123456789000000000000000";

    private static final String CONTRACTOR_NAME = "Contractor Sp. z o.o.";
    private static final String CONTRACTOR_NIP_ENC = "contractor-enc-nip";
    private static final String CONTRACTOR_NIP_PLAIN = "2222222222";

    private static final String BUYER_NAME_OVERRIDE = "Overridden Buyer Name";
    private static final String BUYER_NIP_OVERRIDE = "3333333333";
    private static final String BUYER_NIP_OVERRIDE_ENC = "enc-3333333333";

    private static final String CURRENCY = "PLN";
    private static final String NOTES = "Some invoice notes";

    private static final AddressEmbeddable SELLER_ADDRESS_EMB = AddressEmbeddable.builder()
            .street("Seller Street")
            .buildingNumber("1")
            .apartmentNumber("2")
            .postalCode("00-001")
            .city("Warsaw")
            .country("Poland")
            .build();

    private static final AddressEmbeddable BUYER_ADDRESS_EMB = AddressEmbeddable.builder()
            .street("Buyer Street")
            .buildingNumber("10")
            .apartmentNumber("5")
            .postalCode("00-002")
            .city("Krakow")
            .country("Poland")
            .build();

    private static final AddressDto SELLER_ADDRESS_DTO = new AddressDto(
            "Seller Street",
            "1",
            "2",
            "00-001",
            "Warsaw",
            "Poland"
    );

    private static final AddressDto BUYER_ADDRESS_DTO = new AddressDto(
            "Buyer Street",
            "10",
            "5",
            "00-002",
            "Krakow",
            "Poland"
    );

    @Test
    @DisplayName("createEmptyInvoiceEntity should use overrides and encrypt buyer NIP from request")
    void createEmptyInvoiceEntityShouldUseOverridesAndEncryptBuyerNipFromRequest() {
        InvoiceCreateRequest req = new InvoiceCreateRequest(
                SELLER_PROFILE_ID,
                CONTRACTOR_ID,
                BUYER_NAME_OVERRIDE,
                BUYER_NIP_OVERRIDE,
                null,
                ISSUE_DATE,
                SALE_DATE,
                DUE_DATE,
                PaymentMethod.BANK_TRANSFER,
                CURRENCY,
                NOTES,
                true,
                false,
                List.of()
        );

        SellerProfileEntity sellerProfile = new SellerProfileEntity();
        sellerProfile.setId(SELLER_PROFILE_ID);
        sellerProfile.setName(SELLER_NAME);
        sellerProfile.setNipEncrypted(SELLER_NIP_ENC);
        sellerProfile.setAddress(SELLER_ADDRESS_EMB);
        sellerProfile.setBankAccount(SELLER_BANK_ACCOUNT);

        ContractorEntity contractor = new ContractorEntity();
        contractor.setId(CONTRACTOR_ID);
        contractor.setName(CONTRACTOR_NAME);
        contractor.setNipEncrypted(CONTRACTOR_NIP_ENC);
        contractor.setAddress(BUYER_ADDRESS_EMB);

        when(cryptoService.encrypt(BUYER_NIP_OVERRIDE)).thenReturn(BUYER_NIP_OVERRIDE_ENC);

        InvoiceEntity result = mapper.createEmptyInvoiceEntity(req, sellerProfile, contractor, GENERATED_NUMBER);

        assertAll(
                () -> assertThat(result.getSellerProfile()).isEqualTo(sellerProfile),
                () -> assertThat(result.getOrganization()).isEqualTo(sellerProfile.getOrganization()),
                () -> assertThat(result.getContractor()).isEqualTo(contractor),
                () -> assertThat(result.getNumber()).isEqualTo(GENERATED_NUMBER),
                () -> assertThat(result.getIssueDate()).isEqualTo(ISSUE_DATE),
                () -> assertThat(result.getSaleDate()).isEqualTo(SALE_DATE),
                () -> assertThat(result.getDueDate()).isEqualTo(DUE_DATE),
                () -> assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER),
                () -> assertThat(result.getCurrency()).isEqualTo(CURRENCY),
                () -> assertThat(result.getStatus()).isEqualTo(InvoiceStatus.DRAFT),
                () -> assertThat(result.getSellerName()).isEqualTo(SELLER_NAME),
                () -> assertThat(result.getSellerNipEncrypted()).isEqualTo(SELLER_NIP_ENC),
                () -> assertThat(result.getSellerAddress()).isEqualTo(SELLER_ADDRESS_EMB),
                () -> assertThat(result.getSellerBankAccount()).isEqualTo(SELLER_BANK_ACCOUNT),
                () -> assertThat(result.getBuyerName()).isEqualTo(BUYER_NAME_OVERRIDE),
                () -> assertThat(result.getBuyerNipEncrypted()).isEqualTo(BUYER_NIP_OVERRIDE_ENC),
                () -> assertThat(result.getBuyerAddress()).isEqualTo(BUYER_ADDRESS_EMB),
                () -> assertThat(result.getNotes()).isEqualTo(NOTES),
                () -> assertThat(result.getReverseCharge()).isTrue(),
                () -> assertThat(result.getSplitPayment()).isFalse()
        );

        verify(cryptoService).encrypt(BUYER_NIP_OVERRIDE);
        verify(cryptoService, never()).decrypt(CONTRACTOR_NIP_ENC);
    }

    @Test
    @DisplayName("createEmptyInvoiceEntity should fall back to contractor name and NIP when overrides are null")
    void createEmptyInvoiceEntityShouldUseContractorDataWhenOverridesNull() {
        InvoiceCreateRequest req = new InvoiceCreateRequest(
                SELLER_PROFILE_ID,
                CONTRACTOR_ID,
                null,
                null,
                null,
                ISSUE_DATE,
                SALE_DATE,
                DUE_DATE,
                PaymentMethod.CASH,
                CURRENCY,
                null,
                null,
                null,
                List.of()
        );

        SellerProfileEntity sellerProfile = new SellerProfileEntity();
        sellerProfile.setId(SELLER_PROFILE_ID);
        sellerProfile.setName(SELLER_NAME);
        sellerProfile.setNipEncrypted(SELLER_NIP_ENC);
        sellerProfile.setAddress(SELLER_ADDRESS_EMB);
        sellerProfile.setBankAccount(SELLER_BANK_ACCOUNT);

        ContractorEntity contractor = new ContractorEntity();
        contractor.setId(CONTRACTOR_ID);
        contractor.setName(CONTRACTOR_NAME);
        contractor.setNipEncrypted(CONTRACTOR_NIP_ENC);
        contractor.setAddress(BUYER_ADDRESS_EMB);

        when(cryptoService.decrypt(CONTRACTOR_NIP_ENC)).thenReturn(CONTRACTOR_NIP_PLAIN);
        when(cryptoService.encrypt(CONTRACTOR_NIP_PLAIN)).thenReturn("enc-from-contractor");

        InvoiceEntity result = mapper.createEmptyInvoiceEntity(req, sellerProfile, contractor, GENERATED_NUMBER);

        assertAll(
                () -> assertThat(result.getBuyerName()).isEqualTo(CONTRACTOR_NAME),
                () -> assertThat(result.getBuyerNipEncrypted()).isEqualTo("enc-from-contractor"),
                () -> assertThat(result.getBuyerAddress()).isEqualTo(BUYER_ADDRESS_EMB),
                () -> assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.CASH),
                () -> assertThat(result.getNotes()).isNull(),
                () -> assertThat(result.getReverseCharge()).isFalse(),
                () -> assertThat(result.getSplitPayment()).isFalse()
        );

        verify(cryptoService).decrypt(CONTRACTOR_NIP_ENC);
        verify(cryptoService).encrypt(CONTRACTOR_NIP_PLAIN);
    }

    @Test
    @DisplayName("createEmptyInvoiceEntity should set buyerNipEncrypted to null when both override and contractor NIP are null")
    void createEmptyInvoiceEntityShouldSetBuyerNipEncryptedNullWhenNoNip() {
        InvoiceCreateRequest req = new InvoiceCreateRequest(
                SELLER_PROFILE_ID,
                CONTRACTOR_ID,
                null,
                null,
                null,
                ISSUE_DATE,
                SALE_DATE,
                DUE_DATE,
                PaymentMethod.BANK_TRANSFER,
                CURRENCY,
                null,
                false,
                false,
                List.of()
        );

        SellerProfileEntity sellerProfile = new SellerProfileEntity();
        sellerProfile.setId(SELLER_PROFILE_ID);
        sellerProfile.setName(SELLER_NAME);
        sellerProfile.setNipEncrypted(SELLER_NIP_ENC);
        sellerProfile.setAddress(SELLER_ADDRESS_EMB);
        sellerProfile.setBankAccount(SELLER_BANK_ACCOUNT);

        ContractorEntity contractor = new ContractorEntity();
        contractor.setId(CONTRACTOR_ID);
        contractor.setName(CONTRACTOR_NAME);
        contractor.setNipEncrypted(null);
        contractor.setAddress(BUYER_ADDRESS_EMB);

        InvoiceEntity result = mapper.createEmptyInvoiceEntity(req, sellerProfile, contractor, GENERATED_NUMBER);

        assertAll(
                () -> assertThat(result.getBuyerNipEncrypted()).isNull(),
                () -> assertThat(result.getBuyerName()).isEqualTo(CONTRACTOR_NAME)
        );

        verify(cryptoService, never()).decrypt(anyString());
        verify(cryptoService, never()).encrypt(anyString());
    }

    @Test
    @DisplayName("toItemEntity should map all fields correctly")
    void toItemEntityShouldMapAllFields() {
        InvoiceEntity invoice = new InvoiceEntity();
        InvoiceItemCreateRequest req = new InvoiceItemCreateRequest(
                "Item 1",
                new BigDecimal("2.00"),
                "pcs",
                new BigDecimal("100.00"),
                "23"
        );

        BigDecimal netTotal = new BigDecimal("200.00");
        BigDecimal vatAmount = new BigDecimal("46.00");
        BigDecimal grossTotal = new BigDecimal("246.00");

        InvoiceItemEntity result = mapper.toItemEntity(req, invoice, netTotal, vatAmount, grossTotal);

        assertAll(
                () -> assertThat(result.getInvoice()).isEqualTo(invoice),
                () -> assertThat(result.getDescription()).isEqualTo("Item 1"),
                () -> assertThat(result.getQuantity()).isEqualByComparingTo("2.00"),
                () -> assertThat(result.getUnit()).isEqualTo("pcs"),
                () -> assertThat(result.getNetUnitPrice()).isEqualByComparingTo("100.00"),
                () -> assertThat(result.getVatRate()).isEqualTo("23"),
                () -> assertThat(result.getNetTotal()).isEqualByComparingTo("200.00"),
                () -> assertThat(result.getVatAmount()).isEqualByComparingTo("46.00"),
                () -> assertThat(result.getGrossTotal()).isEqualByComparingTo("246.00")
        );
    }

    @Test
    @DisplayName("toResponse should map all fields, decrypt seller and buyer NIP and map items")
    void toResponseShouldMapAllFieldsAndDecryptNips() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(1L);
        invoice.setNumber(GENERATED_NUMBER);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssueDate(ISSUE_DATE);
        invoice.setSaleDate(SALE_DATE);
        invoice.setDueDate(DUE_DATE);
        invoice.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        invoice.setCurrency(CURRENCY);

        invoice.setSellerName(SELLER_NAME);
        invoice.setSellerNipEncrypted(SELLER_NIP_ENC);
        invoice.setSellerAddress(SELLER_ADDRESS_EMB);
        invoice.setSellerBankAccount(SELLER_BANK_ACCOUNT);

        invoice.setBuyerName(CONTRACTOR_NAME);
        invoice.setBuyerNipEncrypted(CONTRACTOR_NIP_ENC);
        invoice.setBuyerAddress(BUYER_ADDRESS_EMB);

        invoice.setNotes(NOTES);
        invoice.setReverseCharge(true);
        invoice.setSplitPayment(false);

        invoice.setTotalNet(new BigDecimal("200.00"));
        invoice.setTotalVat(new BigDecimal("46.00"));
        invoice.setTotalGross(new BigDecimal("246.00"));

        InvoiceItemEntity item1 = InvoiceItemEntity.builder()
                .id(100L)
                .invoice(invoice)
                .description("Item 1")
                .quantity(new BigDecimal("2.00"))
                .unit("pcs")
                .netUnitPrice(new BigDecimal("100.00"))
                .vatRate("23")
                .netTotal(new BigDecimal("200.00"))
                .vatAmount(new BigDecimal("46.00"))
                .grossTotal(new BigDecimal("246.00"))
                .build();

        InvoiceItemEntity item2 = InvoiceItemEntity.builder()
                .id(101L)
                .invoice(invoice)
                .description("Item 2")
                .quantity(new BigDecimal("1.00"))
                .unit("pcs")
                .netUnitPrice(new BigDecimal("50.00"))
                .vatRate("8")
                .netTotal(new BigDecimal("50.00"))
                .vatAmount(new BigDecimal("4.00"))
                .grossTotal(new BigDecimal("54.00"))
                .build();

        invoice.setItems(List.of(item1, item2));

        when(addressMapper.toDto(SELLER_ADDRESS_EMB)).thenReturn(SELLER_ADDRESS_DTO);
        when(addressMapper.toDto(BUYER_ADDRESS_EMB)).thenReturn(BUYER_ADDRESS_DTO);
        when(cryptoService.decrypt(SELLER_NIP_ENC)).thenReturn(SELLER_NIP_PLAIN);
        when(cryptoService.decrypt(CONTRACTOR_NIP_ENC)).thenReturn(CONTRACTOR_NIP_PLAIN);

        InvoiceResponse response = mapper.toResponse(invoice);

        assertAll(
                () -> assertThat(response.id()).isEqualTo(1L),
                () -> assertThat(response.number()).isEqualTo(GENERATED_NUMBER),
                () -> assertThat(response.status()).isEqualTo(InvoiceStatus.DRAFT),
                () -> assertThat(response.issueDate()).isEqualTo(ISSUE_DATE),
                () -> assertThat(response.saleDate()).isEqualTo(SALE_DATE),
                () -> assertThat(response.dueDate()).isEqualTo(DUE_DATE),
                () -> assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER),
                () -> assertThat(response.currency()).isEqualTo(CURRENCY),
                () -> assertThat(response.sellerName()).isEqualTo(SELLER_NAME),
                () -> assertThat(response.sellerNip()).isEqualTo(SELLER_NIP_PLAIN),
                () -> assertThat(response.sellerAddress()).isEqualTo(SELLER_ADDRESS_DTO),
                () -> assertThat(response.sellerBankAccount()).isEqualTo(SELLER_BANK_ACCOUNT),
                () -> assertThat(response.buyerName()).isEqualTo(CONTRACTOR_NAME),
                () -> assertThat(response.buyerNip()).isEqualTo(CONTRACTOR_NIP_PLAIN),
                () -> assertThat(response.buyerAddress()).isEqualTo(BUYER_ADDRESS_DTO),
                () -> assertThat(response.notes()).isEqualTo(NOTES),
                () -> assertThat(response.reverseCharge()).isTrue(),
                () -> assertThat(response.splitPayment()).isFalse(),
                () -> assertThat(response.totalNet()).isEqualByComparingTo("200.00"),
                () -> assertThat(response.totalVat()).isEqualByComparingTo("46.00"),
                () -> assertThat(response.totalGross()).isEqualByComparingTo("246.00"),
                () -> assertThat(response.items()).hasSize(2)
        );

        InvoiceItemResponse r1 = response.items().get(0);
        InvoiceItemResponse r2 = response.items().get(1);

        assertAll(
                () -> assertThat(r1.id()).isEqualTo(100L),
                () -> assertThat(r1.description()).isEqualTo("Item 1"),
                () -> assertThat(r1.quantity()).isEqualByComparingTo("2.00"),
                () -> assertThat(r1.unit()).isEqualTo("pcs"),
                () -> assertThat(r1.netUnitPrice()).isEqualByComparingTo("100.00"),
                () -> assertThat(r1.vatRate()).isEqualTo("23"),
                () -> assertThat(r1.netTotal()).isEqualByComparingTo("200.00"),
                () -> assertThat(r1.vatAmount()).isEqualByComparingTo("46.00"),
                () -> assertThat(r1.grossTotal()).isEqualByComparingTo("246.00"),
                () -> assertThat(r2.id()).isEqualTo(101L),
                () -> assertThat(r2.description()).isEqualTo("Item 2"),
                () -> assertThat(r2.quantity()).isEqualByComparingTo("1.00"),
                () -> assertThat(r2.unit()).isEqualTo("pcs"),
                () -> assertThat(r2.netUnitPrice()).isEqualByComparingTo("50.00"),
                () -> assertThat(r2.vatRate()).isEqualTo("8"),
                () -> assertThat(r2.netTotal()).isEqualByComparingTo("50.00"),
                () -> assertThat(r2.vatAmount()).isEqualByComparingTo("4.00"),
                () -> assertThat(r2.grossTotal()).isEqualByComparingTo("54.00")
        );

        verify(addressMapper).toDto(SELLER_ADDRESS_EMB);
        verify(addressMapper).toDto(BUYER_ADDRESS_EMB);
        verify(cryptoService).decrypt(SELLER_NIP_ENC);
        verify(cryptoService).decrypt(CONTRACTOR_NIP_ENC);
    }

    @Test
    @DisplayName("toResponse should not decrypt buyer NIP when buyerNipEncrypted is null")
    void toResponseShouldNotDecryptBuyerNipWhenEncryptedNull() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(1L);
        invoice.setNumber(GENERATED_NUMBER);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssueDate(ISSUE_DATE);
        invoice.setSaleDate(SALE_DATE);
        invoice.setDueDate(DUE_DATE);
        invoice.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        invoice.setCurrency(CURRENCY);
        invoice.setSellerName(SELLER_NAME);
        invoice.setSellerNipEncrypted(SELLER_NIP_ENC);
        invoice.setSellerAddress(SELLER_ADDRESS_EMB);
        invoice.setSellerBankAccount(SELLER_BANK_ACCOUNT);
        invoice.setBuyerName(CONTRACTOR_NAME);
        invoice.setBuyerNipEncrypted(null);
        invoice.setBuyerAddress(BUYER_ADDRESS_EMB);
        invoice.setNotes(NOTES);
        invoice.setReverseCharge(false);
        invoice.setSplitPayment(false);

        invoice.setTotalNet(new BigDecimal("200.00"));
        invoice.setTotalVat(new BigDecimal("46.00"));
        invoice.setTotalGross(new BigDecimal("246.00"));

        invoice.setItems(List.of());

        when(addressMapper.toDto(SELLER_ADDRESS_EMB)).thenReturn(SELLER_ADDRESS_DTO);
        when(addressMapper.toDto(BUYER_ADDRESS_EMB)).thenReturn(BUYER_ADDRESS_DTO);
        when(cryptoService.decrypt(SELLER_NIP_ENC)).thenReturn(SELLER_NIP_PLAIN);

        InvoiceResponse response = mapper.toResponse(invoice);

        assertAll(
                () -> assertThat(response.sellerNip()).isEqualTo(SELLER_NIP_PLAIN),
                () -> assertThat(response.buyerNip()).isNull()
        );

        verify(cryptoService).decrypt(SELLER_NIP_ENC);
        verify(cryptoService, times(1)).decrypt(anyString());
    }
}
