package com.softwaremind.invoicedocbackend.tenant;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class SellerProfileRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("invoicedoc")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        System.out.println(">>> Testcontainers JDBC URL (SellerProfileRepositoryTest) = " + postgres.getJdbcUrl());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("app.jwt.secret", () -> "SuperTajnyKluczJWTDoTestowMusibycdlugijakszalony");
        registry.add("app.crypto.secret", () -> "NajwazniejszySekretKryptoDoTestowJestDlugiiBezpieczny");
    }

    @Autowired
    private SellerProfileRepository sellerProfileRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private static final String DEFAULT_CURRENCY = "PLN";
    private static final int DEFAULT_TERM_DAYS = 14;

    private static AddressEmbeddable sampleAddress() {
        return AddressEmbeddable.builder()
                .street("Test Street")
                .buildingNumber("12A")
                .apartmentNumber("7")
                .postalCode("00-001")
                .city("Warsaw")
                .country("Poland")
                .build();
    }

    private OrganizationEntity createAndSaveOrganization(String name) {
        OrganizationEntity org = OrganizationEntity.builder()
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
        return organizationRepository.saveAndFlush(org);
    }

    private SellerProfileEntity createSellerProfile(OrganizationEntity org, String nameSuffix) {
        return SellerProfileEntity.builder()
                .organization(org)
                .name("Seller " + nameSuffix)
                .nipEncrypted("enc-" + nameSuffix)
                .regon("regon-" + nameSuffix)
                .krs("krs-" + nameSuffix)
                .bankName("Bank " + nameSuffix)
                .bankAccount("PL0000000000000000000000000" + nameSuffix)
                .address(sampleAddress())
                .defaultCurrency(DEFAULT_CURRENCY)
                .defaultPaymentTermDays(DEFAULT_TERM_DAYS)
                .logoPath("/logos/seller-" + nameSuffix + ".png")
                .build();
    }

    @Test
    @DisplayName("findByOrganizationId should return all seller profiles for given organization")
    void findByOrganizationIdShouldReturnProfilesForOrganization() {
        sellerProfileRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org1 = createAndSaveOrganization("Org One");
        OrganizationEntity org2 = createAndSaveOrganization("Org Two");

        SellerProfileEntity p1 = createSellerProfile(org1, "1");
        SellerProfileEntity p2 = createSellerProfile(org1, "2");
        SellerProfileEntity p3 = createSellerProfile(org2, "3");

        sellerProfileRepository.saveAll(List.of(p1, p2, p3));
        sellerProfileRepository.flush();

        List<SellerProfileEntity> result = sellerProfileRepository.findByOrganizationId(org1.getId());

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result)
                        .allSatisfy(p -> assertThat(p.getOrganization().getId()).isEqualTo(org1.getId()))
        );
    }

    @Test
    @DisplayName("findByOrganizationId should return empty list when organization has no profiles")
    void findByOrganizationIdShouldReturnEmptyWhenNoProfiles() {
        sellerProfileRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("Empty Org");

        List<SellerProfileEntity> result = sellerProfileRepository.findByOrganizationId(org.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByOrganizationIdAndId should return seller profile when organizationId and id match")
    void findByOrganizationIdAndIdShouldReturnProfileWhenMatch() {
        sellerProfileRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("Org Match");

        SellerProfileEntity p1 = createSellerProfile(org, "A");
        SellerProfileEntity p2 = createSellerProfile(org, "B");

        sellerProfileRepository.saveAll(List.of(p1, p2));
        sellerProfileRepository.flush();

        Long targetId = p1.getId();

        Optional<SellerProfileEntity> found =
                sellerProfileRepository.findByOrganizationIdAndId(org.getId(), targetId);

        assertAll(
                () -> assertThat(found).isPresent(),
                () -> assertThat(found.get().getId()).isEqualTo(targetId),
                () -> assertThat(found.get().getOrganization().getId()).isEqualTo(org.getId())
        );
    }

    @Test
    @DisplayName("findByOrganizationIdAndId should return empty when orgId does not match seller profile's organization")
    void findByOrganizationIdAndIdShouldReturnEmptyWhenOrgDoesNotMatch() {
        sellerProfileRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org1 = createAndSaveOrganization("Org One");
        OrganizationEntity org2 = createAndSaveOrganization("Org Two");

        SellerProfileEntity profileForOrg1 = createSellerProfile(org1, "X");
        sellerProfileRepository.saveAndFlush(profileForOrg1);

        Optional<SellerProfileEntity> found =
                sellerProfileRepository.findByOrganizationIdAndId(org2.getId(), profileForOrg1.getId());

        assertThat(found).isEmpty();
    }
}
