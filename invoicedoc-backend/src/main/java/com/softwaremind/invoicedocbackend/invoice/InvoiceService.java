package com.softwaremind.invoicedocbackend.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.contractor.ContractorRepository;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.invoice.mapper.InvoiceMapper;
import com.softwaremind.invoicedocbackend.security.CurrentUser;
import com.softwaremind.invoicedocbackend.security.CurrentUserProvider;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileRepository;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final ContractorRepository contractorRepository;
    private final InvoiceNumberGeneratorService numberGenerator;
    private final InvoiceMapper invoiceMapper;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest req) {
        CurrentUser cu = currentUserProvider.getCurrentUser();

        SellerProfileEntity sellerProfile = sellerProfileRepository.findById(req.sellerProfileId())
                .orElseThrow(() -> new IllegalStateException("Seller profile not found"));

        if (!sellerProfile.getOrganization().getId().equals(cu.organizationId())) {
            throw new IllegalStateException("Seller profile does not belong to your organization");
        }

        ContractorEntity contractor = contractorRepository.findById(req.contractorId())
                .orElseThrow(() -> new IllegalStateException("Contractor not found"));

        if (!contractor.getOrganization().getId().equals(cu.organizationId())) {
            throw new IllegalStateException("Contractor does not belong to your organization");
        }

        String number = numberGenerator.generateNumberFor(sellerProfile, req.issueDate());

        InvoiceEntity invoice = invoiceMapper.createEmptyInvoiceEntity(req, sellerProfile, contractor, number);

        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;

        for (InvoiceItemCreateRequest itemReq : req.items()) {
            BigDecimal netTotal = itemReq.netUnitPrice()
                    .multiply(itemReq.quantity())
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal vatAmount = calculateVat(netTotal, itemReq.vatRate());
            BigDecimal grossTotal = netTotal.add(vatAmount);

            totalNet = totalNet.add(netTotal);
            totalVat = totalVat.add(vatAmount);
            totalGross = totalGross.add(grossTotal);

            InvoiceItemEntity itemEntity =
                    invoiceMapper.toItemEntity(itemReq, invoice, netTotal, vatAmount, grossTotal);

            invoice.getItems().add(itemEntity);
        }

        invoice.setTotalNet(totalNet);
        invoice.setTotalVat(totalVat);
        invoice.setTotalGross(totalGross);

        InvoiceEntity saved = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(saved);
    }

    private BigDecimal calculateVat(BigDecimal netTotal, String vatRateStr) {
        if (vatRateStr == null || vatRateStr.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal rate = new BigDecimal(vatRateStr);
            return netTotal
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(LocalDate from, LocalDate to, int page, int size) {
        CurrentUser cu = currentUserProvider.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issueDate"));

        Page<InvoiceEntity> result;

        if (from != null && to != null) {
            result = invoiceRepository.findByOrganizationIdAndIssueDateBetween(
                    cu.organizationId(), from, to, pageable);
        } else {
            result = invoiceRepository.findByOrganizationId(cu.organizationId(), pageable);
        }

        return result.map(invoiceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long id) {
        CurrentUser cu = currentUserProvider.getCurrentUser();
        InvoiceEntity entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));

        if (!entity.getOrganization().getId().equals(cu.organizationId())
                && cu.role() != com.softwaremind.invoicedocbackend.security.UserRole.ADMIN) {
            throw new IllegalStateException("No access to this invoice");
        }

        return invoiceMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public InvoiceEntity getInvoiceEntityForPdf(Long id) {
        CurrentUser cu = currentUserProvider.getCurrentUser();

        InvoiceEntity entity = invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Invoice not found"));

        if (!entity.getOrganization().getId().equals(cu.organizationId())
                && cu.role() != com.softwaremind.invoicedocbackend.security.UserRole.ADMIN) {
            throw new IllegalStateException("No access to this invoice");
        }

        entity.getItems().size();
        return entity;
    }

    @Transactional
    public void deleteInvoice(Long id) {
        CurrentUser cu = currentUserProvider.getCurrentUser();

        InvoiceEntity entity = invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND")
                );

        if (!entity.getOrganization().getId().equals(cu.organizationId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "NO_ACCESS_TO_INVOICE"
            );
        }

        invoiceRepository.delete(entity);
    }
}
