package com.softwaremind.invoicedocbackend.invoice;

import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceNumberGeneratorServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceNumberGeneratorService service;

    private SellerProfileEntity sellerProfileWithOrg(Long orgId) {
        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId)
                .name("Org " + orgId)
                .createdAt(LocalDateTime.now())
                .build();

        return SellerProfileEntity.builder()
                .id(10L)
                .organization(org)
                .name("Seller Profile")
                .nipEncrypted("enc-nip")
                .defaultCurrency("PLN")
                .defaultPaymentTermDays(14)
                .build();
    }

    @Test
    @DisplayName("generateNumberFor should build number FV/YYYY/MM/DD/001 when no invoices for that day")
    void generateNumberForShouldReturnSequence001WhenNoInvoices() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        SellerProfileEntity sellerProfile = sellerProfileWithOrg(1L);

        when(invoiceRepository.countByOrganizationIdAndIssueDateBetween(
                1L, date, date
        )).thenReturn(0L);

        String number = service.generateNumberFor(sellerProfile, date);

        assertAll(
                () -> assertThat(number).isEqualTo("FV/2024/03/15/001"),
                () -> verify(invoiceRepository)
                        .countByOrganizationIdAndIssueDateBetween(1L, date, date)
        );
    }

    @Test
    @DisplayName("generateNumberFor should increment sequence based on existing invoices (NNN = count + 1)")
    void generateNumberForShouldIncrementSequence() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        SellerProfileEntity sellerProfile = sellerProfileWithOrg(5L);

        when(invoiceRepository.countByOrganizationIdAndIssueDateBetween(
                5L, date, date
        )).thenReturn(2L);

        String number = service.generateNumberFor(sellerProfile, date);

        assertAll(
                () -> assertThat(number).isEqualTo("FV/2024/03/15/003"),
                () -> verify(invoiceRepository)
                        .countByOrganizationIdAndIssueDateBetween(5L, date, date)
        );
    }

    @Test
    @DisplayName("generateNumberFor should zero-pad month, day and sequence")
    void generateNumberForShouldZeroPadMonthDayAndSequence() {
        LocalDate date = LocalDate.of(2024, 1, 5);
        SellerProfileEntity sellerProfile = sellerProfileWithOrg(7L);

        when(invoiceRepository.countByOrganizationIdAndIssueDateBetween(
                7L, date, date
        )).thenReturn(0L);

        String number = service.generateNumberFor(sellerProfile, date);

        assertThat(number).isEqualTo("FV/2024/01/05/001");
    }

    @Test
    @DisplayName("generateNumberFor should use current date when issueDate is null and still query with same from/to")
    void generateNumberForShouldUseCurrentDateWhenIssueDateIsNull() {
        SellerProfileEntity sellerProfile = sellerProfileWithOrg(10L);
        when(invoiceRepository.countByOrganizationIdAndIssueDateBetween(
                anyLong(), any(), any()
        )).thenReturn(0L);

        String number = service.generateNumberFor(sellerProfile, null);

        String[] parts = number.split("/");
        assertAll(
                () -> assertThat(parts).hasSize(5),
                () -> assertThat(parts[0]).isEqualTo("FV"),
                () -> assertThat(parts[1]).hasSize(4),
                () -> assertThat(parts[2]).hasSize(2),
                () -> assertThat(parts[3]).hasSize(2),
                () -> assertThat(parts[4]).isEqualTo("001")
        );

        verify(invoiceRepository).countByOrganizationIdAndIssueDateBetween(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }
}
