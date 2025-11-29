package com.softwaremind.invoicedocbackend.importing;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.ContractorRepository;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorCreateRequest;
import com.softwaremind.invoicedocbackend.contractor.mapper.ContractorMapper;
import com.softwaremind.invoicedocbackend.importing.dto.ImportExtraDto;
import com.softwaremind.invoicedocbackend.importing.dto.ImportInvoiceItemDto;
import com.softwaremind.invoicedocbackend.importing.dto.ImportInvoiceMetaDto;
import com.softwaremind.invoicedocbackend.importing.dto.ImportPartyDto;
import com.softwaremind.invoicedocbackend.importing.dto.InvoiceImportDto;
import com.softwaremind.invoicedocbackend.importing.dto.InvoiceImportMapper;
import com.softwaremind.invoicedocbackend.invoice.InvoiceService;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.security.CurrentUser;
import com.softwaremind.invoicedocbackend.security.CurrentUserProvider;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileRepository;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final CurrentUserProvider currentUserProvider;
    private final SellerProfileRepository sellerProfileRepository;
    private final ContractorRepository contractorRepository;
    private final ContractorMapper contractorMapper;
    private final OrganizationRepository organizationRepository;
    private final InvoiceImportMapper importMapper;
    private final InvoiceService invoiceService;

    public InvoiceResponse importFromDto(InvoiceImportDto dto, Long sellerProfileId) {
        CurrentUser cu = currentUserProvider.getCurrentUser();

        SellerProfileEntity seller = sellerProfileRepository.findById(sellerProfileId)
                .orElseThrow(() -> new IllegalStateException("Seller profile not found"));

        if (!seller.getOrganization().getId().equals(cu.organizationId())) {
            throw new IllegalStateException("Seller profile not in your org");
        }

        Long contractorId = ensureContractor(dto);

        InvoiceCreateRequest createReq =
                importMapper.toCreateRequest(dto, sellerProfileId, contractorId);

        return invoiceService.createInvoice(createReq);
    }

    public InvoiceResponse importFromCsv(MultipartFile file, Long sellerProfileId) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new IllegalArgumentException("CSV is empty");
            }

            CSVRecord first = records.get(0);

            ImportPartyDto sellerParty = new ImportPartyDto(
                    "COMPANY",
                    first.get("sellerName"),
                    first.get("sellerNip"),
                    null,
                    new AddressDto(
                            first.get("sellerStreet"),
                            first.get("sellerBuildingNumber"),
                            null,
                            first.get("sellerPostalCode"),
                            first.get("sellerCity"),
                            "PL"
                    ),
                    first.isMapped("sellerBankAccount") ? first.get("sellerBankAccount") : null
            );

            ImportPartyDto buyerParty = new ImportPartyDto(
                    "COMPANY",
                    first.get("buyerName"),
                    first.get("buyerNip"),
                    null,
                    new AddressDto(
                            first.get("buyerStreet"),
                            first.get("buyerBuildingNumber"),
                            null,
                            first.get("buyerPostalCode"),
                            first.get("buyerCity"),
                            "PL"
                    ),
                    null
            );

            ImportInvoiceMetaDto meta = new ImportInvoiceMetaDto(
                    first.get("invoiceNumber"),
                    LocalDate.parse(first.get("issueDate")),
                    LocalDate.parse(first.get("saleDate")),
                    LocalDate.parse(first.get("dueDate")),
                    first.get("paymentMethod"),
                    first.get("currency"),
                    "PL"
            );

            List<ImportInvoiceItemDto> items = new ArrayList<>();
            for (CSVRecord r : records) {
                items.add(new ImportInvoiceItemDto(
                        r.get("itemDescription"),
                        new java.math.BigDecimal(r.get("itemQuantity")),
                        r.get("itemUnit"),
                        new java.math.BigDecimal(r.get("itemNetUnitPrice")),
                        r.get("itemVatRate")
                ));
            }

            ImportExtraDto extra = new ImportExtraDto(
                    first.isMapped("notes") ? first.get("notes") : null,
                    first.isMapped("reverseCharge") ? Boolean.valueOf(first.get("reverseCharge")) : false,
                    first.isMapped("splitPayment") ? Boolean.valueOf(first.get("splitPayment")) : false
            );

            InvoiceImportDto dto = new InvoiceImportDto(sellerParty, buyerParty, meta, items, extra);
            return importFromDto(dto, sellerProfileId);
        }
    }


    private Long ensureContractor(InvoiceImportDto dto) {
        CurrentUser cu = currentUserProvider.getCurrentUser();
        ImportPartyDto buyer = dto.buyer();

        if (buyer == null) {
            throw new IllegalArgumentException("Buyer data missing");
        }

        return contractorRepository
                .findByOrganizationIdAndNameContainingIgnoreCase(cu.organizationId(), buyer.name())
                .stream()
                .findFirst()
                .map(ContractorEntity::getId)
                .orElseGet(() -> {
                    OrganizationEntity org = organizationRepository.findById(cu.organizationId())
                            .orElseThrow(() -> new IllegalStateException("Organization not found"));

                    ContractorCreateRequest cReq = new ContractorCreateRequest(
                            importMapper.mapBuyerType(buyer),
                            buyer.name(),
                            buyer.nip(),
                            buyer.pesel(),
                            importMapper.toAddressDto(buyer),
                            null,
                            null,
                            false
                    );
                    ContractorEntity entity = contractorMapper.fromCreateRequest(cReq, org);
                    contractorRepository.save(entity);
                    return entity.getId();
                });
    }
}
