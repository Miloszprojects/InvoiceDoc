package com.softwaremind.invoicedocbackend.invoice;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizationEntity organization;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = true)
    private SellerProfileEntity sellerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id")
    private ContractorEntity contractor;

    @Column(nullable = false)
    private String sellerName;

    @Column(nullable = false, name = "seller_nip_encrypted")
    private String sellerNipEncrypted;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "seller_street")),
            @AttributeOverride(name = "buildingNumber", column = @Column(name = "seller_building_number")),
            @AttributeOverride(name = "apartmentNumber", column = @Column(name = "seller_apartment_number")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "seller_postal_code")),
            @AttributeOverride(name = "city", column = @Column(name = "seller_city")),
            @AttributeOverride(name = "country", column = @Column(name = "seller_country"))
    })
    private AddressEmbeddable sellerAddress;

    private String sellerBankAccount;

    @Column(nullable = false)
    private String buyerName;

    @Column(name = "buyer_nip_encrypted")
    private String buyerNipEncrypted;

    @Column(name = "buyer_pesel_encrypted")
    private String buyerPeselEncrypted;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "buyer_street")),
            @AttributeOverride(name = "buildingNumber", column = @Column(name = "buyer_building_number")),
            @AttributeOverride(name = "apartmentNumber", column = @Column(name = "buyer_apartment_number")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "buyer_postal_code")),
            @AttributeOverride(name = "city", column = @Column(name = "buyer_city")),
            @AttributeOverride(name = "country", column = @Column(name = "buyer_country"))
    })
    private AddressEmbeddable buyerAddress;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate saleDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalNet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalVat;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalGross;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Boolean reverseCharge;

    @Column(nullable = false)
    private Boolean splitPayment;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItemEntity> items = new ArrayList<>();
}
