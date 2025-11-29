package com.softwaremind.invoicedocbackend.contractor;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;
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
class ContractorRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("invoicedoc")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        System.out.println(">>> Testcontainers JDBC URL (ContractorRepositoryTest) = " + postgres.getJdbcUrl());
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("app.jwt.secret", () -> "SuperTajnyKluczJWTDoTestowMusibycdlugijakszalony");
        registry.add("app.crypto.secret", () -> "NajwazniejszySekretKryptoDoTestowJestDlugiiBezpieczny");
    }

    @Autowired
    private ContractorRepository contractorRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private OrganizationEntity createAndSaveOrganization(String name) {
        OrganizationEntity org = OrganizationEntity.builder()
                .name(name)
                .createdAt(LocalDateTime.now())
                .build();
        return organizationRepository.saveAndFlush(org);
    }

    private AddressEmbeddable sampleAddress(String citySuffix) {
        return AddressEmbeddable.builder()
                .street("Test Street")
                .buildingNumber("12A")
                .apartmentNumber("7")
                .postalCode("00-001")
                .city("City " + citySuffix)
                .country("Poland")
                .build();
    }

    private ContractorEntity createContractor(
            OrganizationEntity org,
            ContractorType type,
            String name,
            String nipEnc,
            String peselEnc,
            boolean favorite
    ) {
        return ContractorEntity.builder()
                .organization(org)
                .type(type)
                .name(name)
                .nipEncrypted(nipEnc)
                .peselEncrypted(peselEnc)
                .address(sampleAddress(name))
                .email(name.toLowerCase().replace(" ", ".") + "@example.com")
                .phone("+48 123 456 789")
                .favorite(favorite)
                .build();
    }

    @Test
    @DisplayName("findByOrganizationId should return contractors only for given organization")
    void findByOrganizationIdShouldReturnContractorsForOrganization() {
        contractorRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org1 = createAndSaveOrganization("Org One");
        OrganizationEntity org2 = createAndSaveOrganization("Org Two");

        ContractorEntity c1 = createContractor(org1, ContractorType.COMPANY, "ACME", "enc-nip-1", "enc-pesel-1", true);
        ContractorEntity c2 = createContractor(org1, ContractorType.PERSON, "John Doe", "enc-nip-2", "enc-pesel-2", false);
        ContractorEntity c3 = createContractor(org2, ContractorType.COMPANY, "Other Co", "enc-nip-3", "enc-pesel-3", false);

        contractorRepository.saveAll(List.of(c1, c2, c3));
        contractorRepository.flush();

        List<ContractorEntity> result = contractorRepository.findByOrganizationId(org1.getId());

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result)
                        .allSatisfy(c -> assertThat(c.getOrganization().getId()).isEqualTo(org1.getId())),
                () -> assertThat(result)
                        .extracting(ContractorEntity::getName)
                        .containsExactlyInAnyOrder("ACME", "John Doe")
        );
    }

    @Test
    @DisplayName("findByOrganizationId should return empty list when organization has no contractors")
    void findByOrganizationIdShouldReturnEmptyWhenNoContractors() {
        contractorRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("Empty Org");

        List<ContractorEntity> result = contractorRepository.findByOrganizationId(org.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByOrganizationIdAndId should return contractor when orgId and id match")
    void findByOrganizationIdAndIdShouldReturnContractorWhenMatches() {
        contractorRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("Org");
        ContractorEntity contractor = contractorRepository.saveAndFlush(
                createContractor(org, ContractorType.COMPANY, "ACME", "enc-nip", "enc-pesel", true)
        );

        Optional<ContractorEntity> result =
                contractorRepository.findByOrganizationIdAndId(org.getId(), contractor.getId());

        assertAll(
                () -> assertThat(result).isPresent(),
                () -> assertThat(result.get().getId()).isEqualTo(contractor.getId()),
                () -> assertThat(result.get().getOrganization().getId()).isEqualTo(org.getId())
        );
    }

    @Test
    @DisplayName("findByOrganizationIdAndId should return empty when contractor id does not belong to given org")
    void findByOrganizationIdAndIdShouldReturnEmptyWhenOrgDoesNotMatch() {
        contractorRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org1 = createAndSaveOrganization("Org One");
        OrganizationEntity org2 = createAndSaveOrganization("Org Two");

        ContractorEntity contractorInOrg2 = contractorRepository.saveAndFlush(
                createContractor(org2, ContractorType.PERSON, "John Doe", "enc-nip", "enc-pesel", false)
        );

        Optional<ContractorEntity> result =
                contractorRepository.findByOrganizationIdAndId(org1.getId(), contractorInOrg2.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByOrganizationIdAndId should return empty when contractor does not exist")
    void findByOrganizationIdAndIdShouldReturnEmptyWhenContractorNotExist() {
        contractorRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("Org");

        Optional<ContractorEntity> result =
                contractorRepository.findByOrganizationIdAndId(org.getId(), 9999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByOrganizationIdAndNameContainingIgnoreCase should filter by org and name case-insensitively")
    void findByOrganizationIdAndNameContainingIgnoreCaseShouldFilterProperly() {
        contractorRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org1 = createAndSaveOrganization("Org One");
        OrganizationEntity org2 = createAndSaveOrganization("Org Two");

        ContractorEntity c1 = createContractor(org1, ContractorType.COMPANY, "ACME Sp. z o.o.", "enc-nip-1", "enc-pesel-1", true);
        ContractorEntity c2 = createContractor(org1, ContractorType.PERSON, "John Doe", "enc-nip-2", "enc-pesel-2", false);
        ContractorEntity c3 = createContractor(org1, ContractorType.COMPANY, "Acme Partner", "enc-nip-3", "enc-pesel-3", false);
        ContractorEntity c4 = createContractor(org2, ContractorType.COMPANY, "ACME External", "enc-nip-4", "enc-pesel-4", false);

        contractorRepository.saveAll(List.of(c1, c2, c3, c4));
        contractorRepository.flush();

        List<ContractorEntity> result =
                contractorRepository.findByOrganizationIdAndNameContainingIgnoreCase(org1.getId(), "acme");

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result)
                        .extracting(ContractorEntity::getName)
                        .containsExactlyInAnyOrder("ACME Sp. z o.o.", "Acme Partner"),
                () -> assertThat(result)
                        .allSatisfy(c -> assertThat(c.getOrganization().getId()).isEqualTo(org1.getId()))
        );
    }

    @Test
    @DisplayName("findByOrganizationIdAndNameContainingIgnoreCase should return empty when no matches")
    void findByOrganizationIdAndNameContainingIgnoreCaseShouldReturnEmptyWhenNoMatches() {
        contractorRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity org = createAndSaveOrganization("Org");
        ContractorEntity c1 = createContractor(org, ContractorType.COMPANY, "Foo", "enc-nip-1", "enc-pesel-1", true);
        contractorRepository.saveAndFlush(c1);

        List<ContractorEntity> result =
                contractorRepository.findByOrganizationIdAndNameContainingIgnoreCase(org.getId(), "bar");

        assertThat(result).isEmpty();
    }
}
