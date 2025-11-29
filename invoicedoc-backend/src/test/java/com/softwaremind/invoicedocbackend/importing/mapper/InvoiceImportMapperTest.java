package com.softwaremind.invoicedocbackend.importing.mapper;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.contractor.ContractorType;
import com.softwaremind.invoicedocbackend.importing.dto.*;
import com.softwaremind.invoicedocbackend.invoice.PaymentMethod;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoiceImportMapperTest {

    private InvoiceImportMapper mapper;

    private static final Long SELLER_PROFILE_ID = 10L;
    private static final Long CONTRACTOR_ID = 20L;

    private static final String BUYER_NAME = "ACME Sp. z o.o.";
    private static final String BUYER_NIP = "1234567890";
    private static final String BUYER_PESEL = "90010112345";
    private static final String CURRENCY = "PLN";

    private static final LocalDate ISSUE_DATE = LocalDate.of(2024, 1, 10);
    private static final LocalDate SALE_DATE = LocalDate.of(2024, 1, 5);
    private static final LocalDate DUE_DATE = LocalDate.of(2024, 2, 10);

    private static final AddressDto ADDRESS_DTO = new AddressDto(
            "Test Street",
            "12A",
            "7",
            "00-001",
            "Warsaw",
            "Poland"
    );

    private static final String BANK_ACCOUNT = "PL00123456789000000000000000";

    private ImportPartyDto seller;
    private ImportPartyDto buyer;
    private List<ImportInvoiceItemDto> baseItems;

    @BeforeAll
    void setUp() {
        mapper = new InvoiceImportMapper();

        seller = new ImportPartyDto(
                "COMPANY",
                "Seller Sp. z o.o.",
                "9999999999",
                null,
                ADDRESS_DTO,
                BANK_ACCOUNT
        );

        buyer = new ImportPartyDto(
                "COMPANY",
                BUYER_NAME,
                BUYER_NIP,
                BUYER_PESEL,
                ADDRESS_DTO,
                BANK_ACCOUNT
        );

        ImportInvoiceItemDto item1 = new ImportInvoiceItemDto(
                "Item 1",
                new BigDecimal("2.00"),
                "pcs",
                new BigDecimal("100.00"),
                "23"
        );
        ImportInvoiceItemDto item2 = new ImportInvoiceItemDto(
                "Item 2",
                new BigDecimal("1.00"),
                "pcs",
                new BigDecimal("50.00"),
                "8"
        );

        baseItems = List.of(item1, item2);
    }

    private InvoiceImportDto createImportDto(String paymentMethod, ImportExtraDto extra) {
        ImportInvoiceMetaDto invoiceMeta = new ImportInvoiceMetaDto(
                "INV-001",
                ISSUE_DATE,
                SALE_DATE,
                DUE_DATE,
                paymentMethod,
                CURRENCY,
                "PL"
        );

        return new InvoiceImportDto(
                seller,
                buyer,
                invoiceMeta,
                baseItems,
                extra
        );
    }

    @Test
    @DisplayName("toCreateRequest should map all fields and preserve items when paymentMethod is valid")
    void toCreateRequestShouldMapAllFieldsWithValidPaymentMethod() {
        ImportExtraDto extra = new ImportExtraDto(
                "Some notes",
                true,
                false
        );
        InvoiceImportDto importDto = createImportDto(PaymentMethod.BANK_TRANSFER.name(), extra);

        InvoiceCreateRequest result = mapper.toCreateRequest(importDto, SELLER_PROFILE_ID, CONTRACTOR_ID);

        assertAll(
                () -> assertThat(result.sellerProfileId()).isEqualTo(SELLER_PROFILE_ID),
                () -> assertThat(result.contractorId()).isEqualTo(CONTRACTOR_ID),
                () -> assertThat(result.buyerNameOverride()).isEqualTo(BUYER_NAME),
                () -> assertThat(result.buyerNipOverride()).isEqualTo(BUYER_NIP),
                () -> assertThat(result.buyerPeselOverride()).isEqualTo(BUYER_PESEL),
                () -> assertThat(result.issueDate()).isEqualTo(ISSUE_DATE),
                () -> assertThat(result.saleDate()).isEqualTo(SALE_DATE),
                () -> assertThat(result.dueDate()).isEqualTo(DUE_DATE),
                () -> assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER),
                () -> assertThat(result.currency()).isEqualTo(CURRENCY),
                () -> assertThat(result.notes()).isEqualTo("Some notes"),
                () -> assertThat(result.reverseCharge()).isTrue(),
                () -> assertThat(result.splitPayment()).isFalse(),
                () -> assertThat(result.items()).hasSize(2)
        );

        InvoiceItemCreateRequest mappedItem1 = result.items().get(0);
        InvoiceItemCreateRequest mappedItem2 = result.items().get(1);

        assertAll(
                () -> assertThat(mappedItem1.description()).isEqualTo("Item 1"),
                () -> assertThat(mappedItem1.quantity()).isEqualByComparingTo("2.00"),
                () -> assertThat(mappedItem1.unit()).isEqualTo("pcs"),
                () -> assertThat(mappedItem1.netUnitPrice()).isEqualByComparingTo("100.00"),
                () -> assertThat(mappedItem1.vatRate()).isEqualTo("23"),
                () -> assertThat(mappedItem2.description()).isEqualTo("Item 2"),
                () -> assertThat(mappedItem2.quantity()).isEqualByComparingTo("1.00"),
                () -> assertThat(mappedItem2.unit()).isEqualTo("pcs"),
                () -> assertThat(mappedItem2.netUnitPrice()).isEqualByComparingTo("50.00"),
                () -> assertThat(mappedItem2.vatRate()).isEqualTo("8")
        );
    }

    @Test
    @DisplayName("toCreateRequest should default paymentMethod to BANK_TRANSFER when null")
    void toCreateRequestShouldDefaultPaymentMethodToBankTransferWhenNull() {
        InvoiceImportDto importDto = createImportDto(null, null);

        InvoiceCreateRequest result = mapper.toCreateRequest(importDto, SELLER_PROFILE_ID, CONTRACTOR_ID);

        assertAll(
                () -> assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER),
                () -> assertThat(result.notes()).isNull(),
                () -> assertThat(result.reverseCharge()).isFalse(),
                () -> assertThat(result.splitPayment()).isFalse()
        );
    }

    @Test
    @DisplayName("toCreateRequest should fallback paymentMethod to BANK_TRANSFER when invalid")
    void toCreateRequestShouldFallbackPaymentMethodToBankTransferWhenInvalid() {
        ImportExtraDto extra = new ImportExtraDto(
                null,
                null,
                null
        );
        InvoiceImportDto importDto = createImportDto("NOT_AN_ENUM", extra);

        InvoiceCreateRequest result = mapper.toCreateRequest(importDto, SELLER_PROFILE_ID, CONTRACTOR_ID);

        assertAll(
                () -> assertThat(result.paymentMethod()).isEqualTo(PaymentMethod.BANK_TRANSFER),
                () -> assertThat(result.notes()).isNull(),
                () -> assertThat(result.reverseCharge()).isFalse(),
                () -> assertThat(result.splitPayment()).isFalse()
        );
    }

    @Test
    @DisplayName("toCreateRequest should map reverseCharge and splitPayment when both are true")
    void toCreateRequestShouldMapReverseChargeAndSplitPaymentWhenBothTrue() {
        ImportExtraDto extra = new ImportExtraDto(
                "Extra notes",
                true,
                true
        );
        InvoiceImportDto importDto = createImportDto(PaymentMethod.BANK_TRANSFER.name(), extra);

        InvoiceCreateRequest result = mapper.toCreateRequest(importDto, SELLER_PROFILE_ID, CONTRACTOR_ID);

        assertAll(
                () -> assertThat(result.notes()).isEqualTo("Extra notes"),
                () -> assertThat(result.reverseCharge()).isTrue(),
                () -> assertThat(result.splitPayment()).isTrue()
        );
    }

    @Test
    @DisplayName("mapBuyerType should return COMPANY when buyer is null")
    void mapBuyerTypeShouldReturnCompanyWhenBuyerIsNull() {
        ContractorType result = mapper.mapBuyerType(null);

        assertThat(result).isEqualTo(ContractorType.COMPANY);
    }

    @Test
    @DisplayName("mapBuyerType should return COMPANY when buyer type is null")
    void mapBuyerTypeShouldReturnCompanyWhenBuyerTypeIsNull() {
        ImportPartyDto buyerWithNullType = new ImportPartyDto(
                null,
                BUYER_NAME,
                BUYER_NIP,
                BUYER_PESEL,
                ADDRESS_DTO,
                BANK_ACCOUNT
        );

        ContractorType result = mapper.mapBuyerType(buyerWithNullType);

        assertThat(result).isEqualTo(ContractorType.COMPANY);
    }

    @Test
    @DisplayName("mapBuyerType should return COMPANY when buyer type is 'COMPANY'")
    void mapBuyerTypeShouldReturnCompanyForCompanyType() {
        ImportPartyDto buyerCompany = new ImportPartyDto(
                "COMPANY",
                BUYER_NAME,
                BUYER_NIP,
                BUYER_PESEL,
                ADDRESS_DTO,
                BANK_ACCOUNT
        );

        ContractorType result = mapper.mapBuyerType(buyerCompany);

        assertThat(result).isEqualTo(ContractorType.COMPANY);
    }

    @Test
    @DisplayName("mapBuyerType should return PERSON when buyer type is 'PERSON'")
    void mapBuyerTypeShouldReturnPersonForPersonType() {
        ImportPartyDto buyerPerson = new ImportPartyDto(
                "PERSON",
                BUYER_NAME,
                BUYER_NIP,
                BUYER_PESEL,
                ADDRESS_DTO,
                BANK_ACCOUNT
        );

        ContractorType result = mapper.mapBuyerType(buyerPerson);

        assertThat(result).isEqualTo(ContractorType.PERSON);
    }

    @Test
    @DisplayName("mapBuyerType should return COMPANY when buyer type is unknown")
    void mapBuyerTypeShouldReturnCompanyForUnknownType() {
        ImportPartyDto buyerUnknown = new ImportPartyDto(
                "SOMETHING_ELSE",
                BUYER_NAME,
                BUYER_NIP,
                BUYER_PESEL,
                ADDRESS_DTO,
                BANK_ACCOUNT
        );

        ContractorType result = mapper.mapBuyerType(buyerUnknown);

        assertThat(result).isEqualTo(ContractorType.COMPANY);
    }

    @Test
    @DisplayName("toAddressDto should return address when party and address are not null")
    void toAddressDtoShouldReturnAddressWhenPresent() {
        ImportPartyDto party = new ImportPartyDto(
                "COMPANY",
                BUYER_NAME,
                BUYER_NIP,
                BUYER_PESEL,
                ADDRESS_DTO,
                BANK_ACCOUNT
        );

        AddressDto result = mapper.toAddressDto(party);

        assertThat(result).isSameAs(ADDRESS_DTO);
    }

    @Test
    @DisplayName("toAddressDto should return null when party is null")
    void toAddressDtoShouldReturnNullWhenPartyIsNull() {
        AddressDto result = mapper.toAddressDto(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("toAddressDto should return null when party address is null")
    void toAddressDtoShouldReturnNullWhenAddressIsNull() {
        ImportPartyDto party = new ImportPartyDto(
                "COMPANY",
                BUYER_NAME,
                BUYER_NIP,
                BUYER_PESEL,
                null,
                BANK_ACCOUNT
        );

        AddressDto result = mapper.toAddressDto(party);

        assertThat(result).isNull();
    }
}
