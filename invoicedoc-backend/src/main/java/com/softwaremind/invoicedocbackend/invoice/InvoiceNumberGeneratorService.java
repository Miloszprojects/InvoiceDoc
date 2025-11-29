package com.softwaremind.invoicedocbackend.invoice;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;

@Service
@RequiredArgsConstructor
public class InvoiceNumberGeneratorService {

    private final InvoiceRepository invoiceRepository;

    public String generateNumberFor(SellerProfileEntity sellerProfile, LocalDate issueDate) {
        final String pattern = "FV/{YYYY}/{MM}/{DD}/{NNN}";

        LocalDate date = (issueDate != null) ? issueDate : LocalDate.now();

        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        Long orgId = sellerProfile.getOrganization().getId();

        long countForDay = invoiceRepository.countByOrganizationIdAndIssueDateBetween(
                orgId,
                date,
                date
        );

        long sequence = countForDay + 1;

        String yearStr = String.valueOf(year);
        String monthStr = String.format("%02d", month);
        String dayStr = String.format("%02d", day);
        String seqStr = String.format("%03d", sequence);

        return pattern
                .replace("{YYYY}", yearStr)
                .replace("{MM}", monthStr)
                .replace("{DD}", dayStr)
                .replace("{NNN}", seqStr);
    }
}
