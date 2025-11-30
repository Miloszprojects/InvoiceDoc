package com.softwaremind.invoicedocbackend.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoice_items")
public class InvoiceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private InvoiceEntity invoice;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    private String unit;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netUnitPrice;

    @Column(nullable = false, length = 10)
    private String vatRate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal netTotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal grossTotal;
}
