package com.softwaremind.invoicedocbackend.invoice;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.ContractorType;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.softwaremind.invoicedocbackend.invoice.InvoiceStatus.DRAFT;
import static com.softwaremind.invoicedocbackend.invoice.PaymentMethod.BANK_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class InvoiceEntityTest {

    private static final Long ID = 1L;
    private static final String NUMBER = "INV-2024-001";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2024, 1, 10);
    private static final LocalDate SALE_DATE = LocalDate.of(2024, 1, 9);
    private static final LocalDate DUE_DATE = LocalDate.of(2024, 1, 24);
    private static final String CURRENCY = "PLN";
    private static final String SELLER_NAME = "Seller Sp. z o.o.";
    private static final String SELLER_NIP_ENC = "enc-seller-nip";
    private static final String SELLER_BANK = "PL00123456789000000000000000";
    private static final String BUYER_NAME = "Buyer Sp. z o.o.";
    private static final String BUYER_NIP_ENC = "enc-buyer-nip";
    private static final String BUYER_PESEL_ENC = "enc-buyer-pesel";
    private static final BigDecimal TOTAL_NET = new BigDecimal("1000.00");
    private static final BigDecimal TOTAL_VAT = new BigDecimal("230.00");
    private static final BigDecimal TOTAL_GROSS = new BigDecimal("1230.00");
    private static final String NOTES = "Some notes";
    private static final boolean REVERSE_CHARGE = false;
    private static final boolean SPLIT_PAYMENT = true;

    private static AddressEmbeddable sellerAddress() {
        return AddressEmbeddable.builder()
                .street("Seller Street")
                .buildingNumber("10")
                .apartmentNumber("5")
                .postalCode("00-001")
                .city("Warsaw")
                .country("Poland")
                .build();
    }

    private static AddressEmbeddable buyerAddress() {
        return AddressEmbeddable.builder()
                .street("Buyer Street")
                .buildingNumber("20")
                .apartmentNumber("7")
                .postalCode("00-002")
                .city("Krakow")
                .country("Poland")
                .build();
    }

    private static OrganizationEntity organization() {
        return OrganizationEntity.builder()
                .id(100L)
                .name("Org Name")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static SellerProfileEntity sellerProfile(OrganizationEntity org) {
        return SellerProfileEntity.builder()
                .id(200L)
                .organization(org)
                .name("Profile 1")
                .nipEncrypted("enc-profile-nip")
                .defaultCurrency("PLN")
                .defaultPaymentTermDays(14)
                .address(sellerAddress())
                .build();
    }

    private static ContractorEntity contractor(OrganizationEntity org) {
        return ContractorEntity.builder()
                .id(300L)
                .organization(org)
                .type(ContractorType.COMPANY)
                .name("Contractor 1")
                .favorite(false)
                .address(buyerAddress())
                .build();
    }

    @Test
    @DisplayName("Builder should set all fields correctly")
    void builderShouldSetAllFieldsCorrectly() {
        OrganizationEntity org = organization();
        SellerProfileEntity sellerProfile = sellerProfile(org);
        ContractorEntity contractor = contractor(org);

        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(ID)
                .organization(org)
                .sellerProfile(sellerProfile)
                .contractor(contractor)
                .sellerName(SELLER_NAME)
                .sellerNipEncrypted(SELLER_NIP_ENC)
                .sellerAddress(sellerAddress())
                .sellerBankAccount(SELLER_BANK)
                .buyerName(BUYER_NAME)
                .buyerNipEncrypted(BUYER_NIP_ENC)
                .buyerPeselEncrypted(BUYER_PESEL_ENC)
                .buyerAddress(buyerAddress())
                .number(NUMBER)
                .issueDate(ISSUE_DATE)
                .saleDate(SALE_DATE)
                .dueDate(DUE_DATE)
                .paymentMethod(BANK_TRANSFER)
                .currency(CURRENCY)
                .status(DRAFT)
                .totalNet(TOTAL_NET)
                .totalVat(TOTAL_VAT)
                .totalGross(TOTAL_GROSS)
                .notes(NOTES)
                .reverseCharge(REVERSE_CHARGE)
                .splitPayment(SPLIT_PAYMENT)
                .build();

        assertAll(
                () -> assertThat(invoice.getId()).isEqualTo(ID),
                () -> assertThat(invoice.getOrganization()).isSameAs(org),
                () -> assertThat(invoice.getSellerProfile()).isSameAs(sellerProfile),
                () -> assertThat(invoice.getContractor()).isSameAs(contractor),
                () -> assertThat(invoice.getSellerName()).isEqualTo(SELLER_NAME),
                () -> assertThat(invoice.getSellerNipEncrypted()).isEqualTo(SELLER_NIP_ENC),
                () -> assertThat(invoice.getSellerAddress()).usingRecursiveComparison()
                        .isEqualTo(sellerAddress()),
                () -> assertThat(invoice.getSellerBankAccount()).isEqualTo(SELLER_BANK),
                () -> assertThat(invoice.getBuyerName()).isEqualTo(BUYER_NAME),
                () -> assertThat(invoice.getBuyerNipEncrypted()).isEqualTo(BUYER_NIP_ENC),
                () -> assertThat(invoice.getBuyerPeselEncrypted()).isEqualTo(BUYER_PESEL_ENC),
                () -> assertThat(invoice.getBuyerAddress()).usingRecursiveComparison()
                        .isEqualTo(buyerAddress()),
                () -> assertThat(invoice.getNumber()).isEqualTo(NUMBER),
                () -> assertThat(invoice.getIssueDate()).isEqualTo(ISSUE_DATE),
                () -> assertThat(invoice.getSaleDate()).isEqualTo(SALE_DATE),
                () -> assertThat(invoice.getDueDate()).isEqualTo(DUE_DATE),
                () -> assertThat(invoice.getPaymentMethod()).isEqualTo(BANK_TRANSFER),
                () -> assertThat(invoice.getCurrency()).isEqualTo(CURRENCY),
                () -> assertThat(invoice.getStatus()).isEqualTo(DRAFT),
                () -> assertThat(invoice.getTotalNet()).isEqualByComparingTo(TOTAL_NET),
                () -> assertThat(invoice.getTotalVat()).isEqualByComparingTo(TOTAL_VAT),
                () -> assertThat(invoice.getTotalGross()).isEqualByComparingTo(TOTAL_GROSS),
                () -> assertThat(invoice.getNotes()).isEqualTo(NOTES),
                () -> assertThat(invoice.getReverseCharge()).isEqualTo(REVERSE_CHARGE),
                () -> assertThat(invoice.getSplitPayment()).isEqualTo(SPLIT_PAYMENT),
                () -> assertThat(invoice.getItems()).isNotNull(),
                () -> assertThat(invoice.getItems()).isEmpty()
        );
    }

    @Test
    @DisplayName("No-args constructor and setters should correctly populate fields")
    void settersShouldPopulateFieldsCorrectly() {
        OrganizationEntity org = organization();
        SellerProfileEntity sellerProfile = sellerProfile(org);
        ContractorEntity contractor = contractor(org);

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(ID);
        invoice.setOrganization(org);
        invoice.setSellerProfile(sellerProfile);
        invoice.setContractor(contractor);
        invoice.setSellerName(SELLER_NAME);
        invoice.setSellerNipEncrypted(SELLER_NIP_ENC);
        invoice.setSellerAddress(sellerAddress());
        invoice.setSellerBankAccount(SELLER_BANK);
        invoice.setBuyerName(BUYER_NAME);
        invoice.setBuyerNipEncrypted(BUYER_NIP_ENC);
        invoice.setBuyerPeselEncrypted(BUYER_PESEL_ENC);
        invoice.setBuyerAddress(buyerAddress());
        invoice.setNumber(NUMBER);
        invoice.setIssueDate(ISSUE_DATE);
        invoice.setSaleDate(SALE_DATE);
        invoice.setDueDate(DUE_DATE);
        invoice.setPaymentMethod(BANK_TRANSFER);
        invoice.setCurrency(CURRENCY);
        invoice.setStatus(DRAFT);
        invoice.setTotalNet(TOTAL_NET);
        invoice.setTotalVat(TOTAL_VAT);
        invoice.setTotalGross(TOTAL_GROSS);
        invoice.setNotes(NOTES);
        invoice.setReverseCharge(REVERSE_CHARGE);
        invoice.setSplitPayment(SPLIT_PAYMENT);
        invoice.setItems(new java.util.ArrayList<>());

        assertAll(
                () -> assertThat(invoice.getId()).isEqualTo(ID),
                () -> assertThat(invoice.getOrganization()).isSameAs(org),
                () -> assertThat(invoice.getSellerProfile()).isSameAs(sellerProfile),
                () -> assertThat(invoice.getContractor()).isSameAs(contractor),
                () -> assertThat(invoice.getSellerName()).isEqualTo(SELLER_NAME),
                () -> assertThat(invoice.getSellerNipEncrypted()).isEqualTo(SELLER_NIP_ENC),
                () -> assertThat(invoice.getSellerAddress()).usingRecursiveComparison()
                        .isEqualTo(sellerAddress()),
                () -> assertThat(invoice.getSellerBankAccount()).isEqualTo(SELLER_BANK),
                () -> assertThat(invoice.getBuyerName()).isEqualTo(BUYER_NAME),
                () -> assertThat(invoice.getBuyerNipEncrypted()).isEqualTo(BUYER_NIP_ENC),
                () -> assertThat(invoice.getBuyerPeselEncrypted()).isEqualTo(BUYER_PESEL_ENC),
                () -> assertThat(invoice.getBuyerAddress()).usingRecursiveComparison()
                        .isEqualTo(buyerAddress()),
                () -> assertThat(invoice.getNumber()).isEqualTo(NUMBER),
                () -> assertThat(invoice.getIssueDate()).isEqualTo(ISSUE_DATE),
                () -> assertThat(invoice.getSaleDate()).isEqualTo(SALE_DATE),
                () -> assertThat(invoice.getDueDate()).isEqualTo(DUE_DATE),
                () -> assertThat(invoice.getPaymentMethod()).isEqualTo(BANK_TRANSFER),
                () -> assertThat(invoice.getCurrency()).isEqualTo(CURRENCY),
                () -> assertThat(invoice.getStatus()).isEqualTo(DRAFT),
                () -> assertThat(invoice.getTotalNet()).isEqualByComparingTo(TOTAL_NET),
                () -> assertThat(invoice.getTotalVat()).isEqualByComparingTo(TOTAL_VAT),
                () -> assertThat(invoice.getTotalGross()).isEqualByComparingTo(TOTAL_GROSS),
                () -> assertThat(invoice.getNotes()).isEqualTo(NOTES),
                () -> assertThat(invoice.getReverseCharge()).isEqualTo(REVERSE_CHARGE),
                () -> assertThat(invoice.getSplitPayment()).isEqualTo(SPLIT_PAYMENT),
                () -> assertThat(invoice.getItems()).isNotNull(),
                () -> assertThat(invoice.getItems()).isEmpty()
        );
    }

    @Test
    @DisplayName("Items list should be mutable and hold items with back-reference to invoice")
    void itemsListShouldBeMutableAndHoldItemsWithBackReference() {
        OrganizationEntity org = organization();
        InvoiceEntity invoice = InvoiceEntity.builder()
                .organization(org)
                .sellerName(SELLER_NAME)
                .sellerNipEncrypted(SELLER_NIP_ENC)
                .sellerAddress(sellerAddress())
                .buyerName(BUYER_NAME)
                .buyerAddress(buyerAddress())
                .number(NUMBER)
                .issueDate(ISSUE_DATE)
                .saleDate(SALE_DATE)
                .dueDate(DUE_DATE)
                .paymentMethod(BANK_TRANSFER)
                .currency(CURRENCY)
                .status(DRAFT)
                .totalNet(BigDecimal.ZERO)
                .totalVat(BigDecimal.ZERO)
                .totalGross(BigDecimal.ZERO)
                .reverseCharge(false)
                .splitPayment(false)
                .build();

        assertThat(invoice.getItems()).isNotNull();
        assertThat(invoice.getItems()).isEmpty();

        InvoiceItemEntity item1 = InvoiceItemEntity.builder()
                .invoice(invoice)
                .description("Item 1")
                .quantity(new BigDecimal("1.00"))
                .unit("pcs")
                .netUnitPrice(new BigDecimal("100.00"))
                .vatRate("23")
                .netTotal(new BigDecimal("100.00"))
                .vatAmount(new BigDecimal("23.00"))
                .grossTotal(new BigDecimal("123.00"))
                .build();

        InvoiceItemEntity item2 = InvoiceItemEntity.builder()
                .invoice(invoice)
                .description("Item 2")
                .quantity(new BigDecimal("2.50"))
                .unit("pcs")
                .netUnitPrice(new BigDecimal("50.00"))
                .vatRate("23")
                .netTotal(new BigDecimal("125.00"))
                .vatAmount(new BigDecimal("28.75"))
                .grossTotal(new BigDecimal("153.75"))
                .build();

        invoice.getItems().add(item1);
        invoice.getItems().add(item2);

        assertAll(
                () -> assertThat(invoice.getItems()).hasSize(2),
                () -> assertThat(invoice.getItems())
                        .extracting(InvoiceItemEntity::getDescription)
                        .containsExactlyInAnyOrder("Item 1", "Item 2"),
                () -> assertThat(invoice.getItems())
                        .allSatisfy(it -> assertThat(it.getInvoice()).isSameAs(invoice))
        );
    }
}
