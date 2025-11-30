package com.softwaremind.invoicedocbackend.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "seller_profiles")
public class SellerProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private OrganizationEntity organization;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, name = "nip_encrypted")
    private String nipEncrypted;

    private String regon;

    private String krs;

    @Column(name = "bank_name")
    private String bankName;

    private String bankAccount;

    @Embedded
    private AddressEmbeddable address;

    @Column(nullable = false, length = 3)
    private String defaultCurrency;

    @Column(nullable = false)
    private Integer defaultPaymentTermDays;

    private String logoPath;
}