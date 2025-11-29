package com.softwaremind.invoicedocbackend.invoice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class InvoiceItemEntityTest {

    private static final Long ID = 1L;
    private static final String DESCRIPTION = "Test item";
    private static final BigDecimal QUANTITY = new BigDecimal("2.5000");
    private static final String UNIT = "pcs";
    private static final BigDecimal NET_UNIT_PRICE = new BigDecimal("100.00");
    private static final String VAT_RATE = "23";
    private static final BigDecimal NET_TOTAL = new BigDecimal("250.00");
    private static final BigDecimal VAT_AMOUNT = new BigDecimal("57.50");
    private static final BigDecimal GROSS_TOTAL = new BigDecimal("307.50");

    @Test
    @DisplayName("Builder should set all fields correctly")
    void builderShouldSetAllFieldsCorrectly() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(10L);

        InvoiceItemEntity item = InvoiceItemEntity.builder()
                .id(ID)
                .invoice(invoice)
                .description(DESCRIPTION)
                .quantity(QUANTITY)
                .unit(UNIT)
                .netUnitPrice(NET_UNIT_PRICE)
                .vatRate(VAT_RATE)
                .netTotal(NET_TOTAL)
                .vatAmount(VAT_AMOUNT)
                .grossTotal(GROSS_TOTAL)
                .build();

        assertAll(
                () -> assertThat(item.getId()).isEqualTo(ID),
                () -> assertThat(item.getInvoice()).isSameAs(invoice),
                () -> assertThat(item.getDescription()).isEqualTo(DESCRIPTION),
                () -> assertThat(item.getQuantity()).isEqualByComparingTo(QUANTITY),
                () -> assertThat(item.getUnit()).isEqualTo(UNIT),
                () -> assertThat(item.getNetUnitPrice()).isEqualByComparingTo(NET_UNIT_PRICE),
                () -> assertThat(item.getVatRate()).isEqualTo(VAT_RATE),
                () -> assertThat(item.getNetTotal()).isEqualByComparingTo(NET_TOTAL),
                () -> assertThat(item.getVatAmount()).isEqualByComparingTo(VAT_AMOUNT),
                () -> assertThat(item.getGrossTotal()).isEqualByComparingTo(GROSS_TOTAL)
        );
    }

    @Test
    @DisplayName("No-args constructor and setters should correctly populate fields")
    void settersShouldPopulateFieldsCorrectly() {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(20L);

        InvoiceItemEntity item = new InvoiceItemEntity();
        item.setId(ID);
        item.setInvoice(invoice);
        item.setDescription(DESCRIPTION);
        item.setQuantity(QUANTITY);
        item.setUnit(UNIT);
        item.setNetUnitPrice(NET_UNIT_PRICE);
        item.setVatRate(VAT_RATE);
        item.setNetTotal(NET_TOTAL);
        item.setVatAmount(VAT_AMOUNT);
        item.setGrossTotal(GROSS_TOTAL);

        assertAll(
                () -> assertThat(item.getId()).isEqualTo(ID),
                () -> assertThat(item.getInvoice()).isSameAs(invoice),
                () -> assertThat(item.getDescription()).isEqualTo(DESCRIPTION),
                () -> assertThat(item.getQuantity()).isEqualByComparingTo(QUANTITY),
                () -> assertThat(item.getUnit()).isEqualTo(UNIT),
                () -> assertThat(item.getNetUnitPrice()).isEqualByComparingTo(NET_UNIT_PRICE),
                () -> assertThat(item.getVatRate()).isEqualTo(VAT_RATE),
                () -> assertThat(item.getNetTotal()).isEqualByComparingTo(NET_TOTAL),
                () -> assertThat(item.getVatAmount()).isEqualByComparingTo(VAT_AMOUNT),
                () -> assertThat(item.getGrossTotal()).isEqualByComparingTo(GROSS_TOTAL)
        );
    }

    @Test
    @DisplayName("Quantity should support 4 decimal places and totals 2 decimal places")
    void quantityAndTotalsShouldRespectScalesConceptually() {
        BigDecimal quantity = new BigDecimal("1.2345");
        BigDecimal netUnitPrice = new BigDecimal("10.00");
        BigDecimal netTotal = new BigDecimal("12.35"); // zaokrąglone z 12.345

        InvoiceItemEntity item = InvoiceItemEntity.builder()
                .quantity(quantity)
                .netUnitPrice(netUnitPrice)
                .netTotal(netTotal)
                .vatRate("23")
                .vatAmount(new BigDecimal("2.84"))
                .grossTotal(new BigDecimal("15.19"))
                .build();

        assertAll(
                () -> assertThat(item.getQuantity()).isEqualByComparingTo("1.2345"),
                () -> assertThat(item.getNetTotal()).isEqualByComparingTo("12.35"),
                () -> assertThat(item.getVatAmount()).isEqualByComparingTo("2.84"),
                () -> assertThat(item.getGrossTotal()).isEqualByComparingTo("15.19")
        );
    }
}
