package com.softwaremind.invoicedocbackend.invoice;

import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.ContractorRepository;
import com.softwaremind.invoicedocbackend.contractor.ContractorType;
import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.softwaremind.invoicedocbackend.invoice.InvoiceStatus.DRAFT;
import static com.softwaremind.invoicedocbackend.invoice.PaymentMethod.BANK_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class InvoiceRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("invoicedoc")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        System.out.println(">>> Testcontainers JDBC URL (InvoiceRepositoryTest) = " + postgres.getJdbcUrl());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("app.jwt.secret", () -> "SuperTajnyKluczJWTDoTestowMusibycdlugijakszalony");
        registry.add("app.crypto.secret", () -> "NajwazniejszySekretKryptoDoTestowJestDlugiiBezpieczny");
    }

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ContractorRepository contractorRepository;

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private OrganizationEntity org1;
    private OrganizationEntity org2;
    private ContractorEntity contractor1;
    private ContractorEntity contractor2;
    private SellerProfileEntity seller1;
    private SellerProfileEntity seller2;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        contractorRepository.deleteAll();
        sellerProfileRepository.deleteAll();
        organizationRepository.deleteAll();

        String suffix = UUID.randomUUID().toString();

        org1 = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Org One " + suffix)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        org2 = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Org Two " + suffix)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        AddressEmbeddable addr = AddressEmbeddable.builder()
                .street("Street")
                .buildingNumber("1")
                .postalCode("00-000")
                .city("City")
                .country("PL")
                .build();

        contractor1 = contractorRepository.save(
                ContractorEntity.builder()
                        .organization(org1)
                        .type(ContractorType.COMPANY)
                        .name("Contractor 1")
                        .favorite(false)
                        .address(addr)
                        .build()
        );

        contractor2 = contractorRepository.save(
                ContractorEntity.builder()
                        .organization(org2)
                        .type(ContractorType.COMPANY)
                        .name("Contractor 2")
                        .favorite(false)
                        .address(addr)
                        .build()
        );

        seller1 = sellerProfileRepository.save(
                SellerProfileEntity.builder()
                        .organization(org1)
                        .name("Seller 1")
                        .nipEncrypted("enc-nip-1")
                        .defaultCurrency("PLN")
                        .defaultPaymentTermDays(14)
                        .address(addr)
                        .build()
        );

        seller2 = sellerProfileRepository.save(
                SellerProfileEntity.builder()
                        .organization(org2)
                        .name("Seller 2")
                        .nipEncrypted("enc-nip-2")
                        .defaultCurrency("PLN")
                        .defaultPaymentTermDays(14)
                        .address(addr)
                        .build()
        );

        createInvoice(org1, seller1, contractor1,
                "INV-ORG1-001",
                LocalDate.of(2024, 1, 5));

        createInvoice(org1, seller1, contractor1,
                "INV-ORG1-002",
                LocalDate.of(2024, 1, 15));

        createInvoice(org1, seller1, contractor1,
                "INV-ORG1-003",
                LocalDate.of(2024, 2, 1));

        createInvoice(org2, seller2, contractor2,
                "INV-ORG2-001",
                LocalDate.of(2024, 1, 10));
    }

    private InvoiceEntity createInvoice(OrganizationEntity org,
                                        SellerProfileEntity seller,
                                        ContractorEntity contractor,
                                        String number,
                                        LocalDate issueDate) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setOrganization(org);
        invoice.setSellerProfile(seller);
        invoice.setContractor(contractor);
        invoice.setNumber(number);
        invoice.setIssueDate(issueDate);
        invoice.setSaleDate(issueDate);
        invoice.setDueDate(issueDate.plusDays(14));
        invoice.setPaymentMethod(BANK_TRANSFER);
        invoice.setCurrency("PLN");
        invoice.setStatus(DRAFT);
        invoice.setSellerName(seller.getName());
        invoice.setSellerNipEncrypted("enc-nip");
        invoice.setSellerAddress(seller.getAddress());
        invoice.setSellerBankAccount("PL00123456789000000000000000");
        invoice.setBuyerName(contractor.getName());
        invoice.setBuyerAddress(contractor.getAddress());
        invoice.setTotalNet(BigDecimal.ZERO);
        invoice.setTotalVat(BigDecimal.ZERO);
        invoice.setTotalGross(BigDecimal.ZERO);
        invoice.setReverseCharge(false);
        invoice.setSplitPayment(false);
        invoice.setItems(new java.util.ArrayList<>());

        return invoiceRepository.save(invoice);
    }

    @Test
    @DisplayName("findByOrganizationId should return only invoices for given organization")
    void findByOrganizationIdShouldReturnOnlyOrgInvoices() {
        var page = invoiceRepository.findByOrganizationId(
                org1.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(3);
        List<String> numbers = page.getContent().stream()
                .map(InvoiceEntity::getNumber)
                .toList();

        assertThat(numbers).containsExactlyInAnyOrder(
                "INV-ORG1-001",
                "INV-ORG1-002",
                "INV-ORG1-003"
        );
    }

    @Test
    @DisplayName("findByOrganizationIdAndIssueDateBetween should filter by org and date range")
    void findByOrganizationIdAndIssueDateBetweenShouldFilter() {
        LocalDate from = LocalDate.of(2024, 1, 10);
        LocalDate to = LocalDate.of(2024, 1, 31);

        var page = invoiceRepository.findByOrganizationIdAndIssueDateBetween(
                org1.getId(),
                from,
                to,
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getNumber()).isEqualTo("INV-ORG1-002");
    }

    @Test
    @DisplayName("existsByOrganizationIdAndNumber should return true only for matching org and number")
    void existsByOrganizationIdAndNumberShouldWork() {
        boolean existsOrg1 = invoiceRepository.existsByOrganizationIdAndNumber(
                org1.getId(), "INV-ORG1-001");
        boolean existsOrg2WrongNumber = invoiceRepository.existsByOrganizationIdAndNumber(
                org2.getId(), "INV-ORG1-001");
        boolean notExists = invoiceRepository.existsByOrganizationIdAndNumber(
                org1.getId(), "UNKNOWN");

        assertThat(existsOrg1).isTrue();
        assertThat(existsOrg2WrongNumber).isFalse();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("existsByContractorId should return true when any invoice uses given contractor")
    void existsByContractorIdShouldReturnTrueWhenUsed() {
        boolean exists1 = invoiceRepository.existsByContractorId(contractor1.getId());
        boolean exists2 = invoiceRepository.existsByContractorId(contractor2.getId());
        boolean notExists = invoiceRepository.existsByContractorId(9999L);

        assertThat(exists1).isTrue();
        assertThat(exists2).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("clearContractorForInvoices should set contractor to null for all matching invoices")
    void clearContractorForInvoicesShouldNullOutContractor() {
        invoiceRepository.clearContractorForInvoices(contractor1.getId());
        entityManager.clear();

        var page = invoiceRepository.findByOrganizationId(
                org1.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .isNotEmpty()
                .allMatch(i -> i.getContractor() == null);
    }

    @Test
    @Transactional
    @DisplayName("clearSellerProfileForInvoices should set sellerProfile to null for all matching invoices")
    void clearSellerProfileForInvoicesShouldNullOutSellerProfile() {
        invoiceRepository.clearSellerProfileForInvoices(seller1.getId());
        entityManager.clear();

        var page = invoiceRepository.findByOrganizationId(
                org1.getId(),
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .isNotEmpty()
                .allMatch(i -> i.getSellerProfile() == null);
    }

    @Test
    @DisplayName("countByOrganizationIdAndIssueDateBetween should count only invoices in range and org")
    void countByOrganizationIdAndIssueDateBetweenShouldCountProperly() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        long countOrg1 = invoiceRepository.countByOrganizationIdAndIssueDateBetween(
                org1.getId(), from, to);
        long countOrg2 = invoiceRepository.countByOrganizationIdAndIssueDateBetween(
                org2.getId(), from, to);

        assertThat(countOrg1).isEqualTo(2);
        assertThat(countOrg2).isEqualTo(1);
    }
}
