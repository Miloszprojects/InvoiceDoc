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
class InvoiceItemRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("invoicedoc")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        System.out.println(">>> Testcontainers JDBC URL (InvoiceItemRepositoryTest) = " + postgres.getJdbcUrl());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("app.jwt.secret", () -> "SuperTajnyKluczJWTDoTestowMusibycdlugijakszalony");
        registry.add("app.crypto.secret", () -> "NajwazniejszySekretKryptoDoTestowJestDlugiiBezpieczny");
    }

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

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

    private OrganizationEntity org;
    private ContractorEntity contractor;
    private SellerProfileEntity seller;
    private InvoiceEntity invoice;

    @BeforeEach
    void setUp() {
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        contractorRepository.deleteAll();
        sellerProfileRepository.deleteAll();
        organizationRepository.deleteAll();

        String suffix = UUID.randomUUID().toString();

        org = organizationRepository.save(
                OrganizationEntity.builder()
                        .name("Org Items " + suffix)
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

        contractor = contractorRepository.save(
                ContractorEntity.builder()
                        .organization(org)
                        .type(ContractorType.COMPANY)
                        .name("Contractor Items")
                        .favorite(false)
                        .address(addr)
                        .build()
        );

        seller = sellerProfileRepository.save(
                SellerProfileEntity.builder()
                        .organization(org)
                        .name("Seller Items")
                        .nipEncrypted("enc-nip-items")
                        .defaultCurrency("PLN")
                        .defaultPaymentTermDays(14)
                        .address(addr)
                        .build()
        );

        invoice = new InvoiceEntity();
        invoice.setOrganization(org);
        invoice.setSellerProfile(seller);
        invoice.setContractor(contractor);
        invoice.setNumber("INV-ITEMS-001");
        invoice.setIssueDate(LocalDate.of(2024, 1, 10));
        invoice.setSaleDate(LocalDate.of(2024, 1, 10));
        invoice.setDueDate(LocalDate.of(2024, 1, 24));
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

        invoice = invoiceRepository.save(invoice);
    }

    private InvoiceItemEntity createItem(String desc,
                                         BigDecimal qty,
                                         String unit,
                                         BigDecimal netUnitPrice,
                                         String vatRate) {
        BigDecimal netTotal = netUnitPrice.multiply(qty);
        BigDecimal vatAmount = netTotal.multiply(new BigDecimal(vatRate))
                .divide(BigDecimal.valueOf(100));
        BigDecimal grossTotal = netTotal.add(vatAmount);

        InvoiceItemEntity item = InvoiceItemEntity.builder()
                .invoice(invoice)
                .description(desc)
                .quantity(qty)
                .unit(unit)
                .netUnitPrice(netUnitPrice)
                .vatRate(vatRate)
                .netTotal(netTotal)
                .vatAmount(vatAmount)
                .grossTotal(grossTotal)
                .build();

        return invoiceItemRepository.save(item);
    }

    @Test
    @DisplayName("save and findById should persist all item fields and relation to invoice")
    void saveAndFindByIdShouldPersistItemAndRelation() {
        InvoiceItemEntity saved = createItem(
                "Service A",
                new BigDecimal("2.00"),
                "h",
                new BigDecimal("100.00"),
                "23"
        );

        InvoiceItemEntity found = invoiceItemRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getDescription()).isEqualTo("Service A");
        assertThat(found.getQuantity()).isEqualByComparingTo("2.00");
        assertThat(found.getUnit()).isEqualTo("h");
        assertThat(found.getNetUnitPrice()).isEqualByComparingTo("100.00");
        assertThat(found.getVatRate()).isEqualTo("23");
        assertThat(found.getNetTotal()).isEqualByComparingTo("200.00");
        assertThat(found.getVatAmount()).isEqualByComparingTo("46.00");
        assertThat(found.getGrossTotal()).isEqualByComparingTo("246.00");

        assertThat(found.getInvoice()).isNotNull();
        assertThat(found.getInvoice().getId()).isEqualTo(invoice.getId());
    }

    @Test
    @Transactional
    @DisplayName("Invoice should see persisted items in its items collection")
    void invoiceShouldContainPersistedItems() {
        InvoiceItemEntity item1 = createItem(
                "Item 1",
                new BigDecimal("1.00"),
                "pcs",
                new BigDecimal("50.00"),
                "23"
        );
        InvoiceItemEntity item2 = createItem(
                "Item 2",
                new BigDecimal("3.00"),
                "pcs",
                new BigDecimal("10.00"),
                "8"
        );

        entityManager.clear();

        InvoiceEntity reloaded = invoiceRepository.findById(invoice.getId())
                .orElseThrow();

        List<InvoiceItemEntity> items = reloaded.getItems();
        assertThat(items).hasSize(2);

        List<Long> itemIds = items.stream()
                .map(InvoiceItemEntity::getId)
                .toList();

        assertThat(itemIds).containsExactlyInAnyOrder(
                item1.getId(),
                item2.getId()
        );
    }

    @Test
    @DisplayName("delete should remove item from repository")
    void deleteShouldRemoveItem() {
        InvoiceItemEntity saved = createItem(
                "To Delete",
                new BigDecimal("1.00"),
                "pcs",
                new BigDecimal("10.00"),
                "23"
        );

        Long id = saved.getId();
        assertThat(invoiceItemRepository.existsById(id)).isTrue();

        invoiceItemRepository.deleteById(id);

        assertThat(invoiceItemRepository.existsById(id)).isFalse();
    }
}
