package com.softwaremind.invoicedocbackend.invoice.mapper;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.softwaremind.invoicedocbackend.common.AddressEmbeddable;
import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.common.mapper.AddressMapper;
import com.softwaremind.invoicedocbackend.contractor.ContractorEntity;
import com.softwaremind.invoicedocbackend.crypto.CryptoService;
import com.softwaremind.invoicedocbackend.invoice.InvoiceEntity;
import com.softwaremind.invoicedocbackend.invoice.InvoiceItemEntity;
import com.softwaremind.invoicedocbackend.invoice.InvoiceStatus;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemResponse;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.tenant.SellerProfileEntity;

@Component
@RequiredArgsConstructor
public class InvoiceMapper {

    private final AddressMapper addressMapper;
    private final CryptoService cryptoService;

    public InvoiceEntity createEmptyInvoiceEntity(InvoiceCreateRequest req,
                                                  SellerProfileEntity sellerProfile,
                                                  ContractorEntity contractor,
                                                  String generatedNumber) {

        AddressEmbeddable sellerAddr = sellerProfile.getAddress();
        String sellerNipEnc = sellerProfile.getNipEncrypted();

        String buyerName = req.buyerNameOverride() != null ?
                req.buyerNameOverride() : contractor.getName();

        String buyerNipPlain =
                req.buyerNipOverride() != null ? req.buyerNipOverride()
                        : decryptOrNull(contractor.getNipEncrypted());

        String buyerNipEnc = buyerNipPlain != null ? cryptoService.encrypt(buyerNipPlain) : null;

        AddressEmbeddable buyerAddr = contractor.getAddress();

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setSellerProfile(sellerProfile);
        invoice.setOrganization(sellerProfile.getOrganization());
        invoice.setContractor(contractor);

        invoice.setNumber(generatedNumber);
        invoice.setIssueDate(req.issueDate());
        invoice.setSaleDate(req.saleDate());
        invoice.setDueDate(req.dueDate());
        invoice.setPaymentMethod(req.paymentMethod());
        invoice.setCurrency(req.currency());
        invoice.setStatus(InvoiceStatus.DRAFT);

        invoice.setSellerName(sellerProfile.getName());
        invoice.setSellerNipEncrypted(sellerNipEnc);
        invoice.setSellerAddress(sellerAddr);
        invoice.setSellerBankAccount(sellerProfile.getBankAccount());

        invoice.setBuyerName(buyerName);
        invoice.setBuyerNipEncrypted(buyerNipEnc);
        invoice.setBuyerAddress(buyerAddr);

        invoice.setNotes(req.notes());
        invoice.setReverseCharge(Boolean.TRUE.equals(req.reverseCharge()));
        invoice.setSplitPayment(Boolean.TRUE.equals(req.splitPayment()));

        return invoice;
    }

    private String decryptOrNull(String enc) {
        return enc != null ? cryptoService.decrypt(enc) : null;
    }

    public InvoiceItemEntity toItemEntity(InvoiceItemCreateRequest req,
                                          InvoiceEntity invoice,
                                          BigDecimal netTotal,
                                          BigDecimal vatAmount,
                                          BigDecimal grossTotal) {
        return InvoiceItemEntity.builder()
                .invoice(invoice)
                .description(req.description())
                .quantity(req.quantity())
                .unit(req.unit())
                .netUnitPrice(req.netUnitPrice())
                .vatRate(req.vatRate())
                .netTotal(netTotal)
                .vatAmount(vatAmount)
                .grossTotal(grossTotal)
                .build();
    }

    public InvoiceResponse toResponse(InvoiceEntity entity) {
        List<InvoiceItemResponse> items = entity.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        AddressDto sellerAddr = addressMapper.toDto(entity.getSellerAddress());
        AddressDto buyerAddr = addressMapper.toDto(entity.getBuyerAddress());

        return new InvoiceResponse(
                entity.getId(),
                entity.getNumber(),
                entity.getStatus(),
                entity.getIssueDate(),
                entity.getSaleDate(),
                entity.getDueDate(),
                entity.getPaymentMethod(),
                entity.getCurrency(),
                entity.getSellerName(),
                cryptoService.decrypt(entity.getSellerNipEncrypted()),
                sellerAddr,
                entity.getSellerBankAccount(),
                entity.getBuyerName(),
                entity.getBuyerNipEncrypted() != null ? cryptoService.decrypt(entity.getBuyerNipEncrypted()) : null,
                buyerAddr,
                entity.getNotes(),
                entity.getReverseCharge(),
                entity.getSplitPayment(),
                entity.getTotalNet(),
                entity.getTotalVat(),
                entity.getTotalGross(),
                items
        );
    }

    private InvoiceItemResponse toItemResponse(InvoiceItemEntity i) {
        return new InvoiceItemResponse(
                i.getId(),
                i.getDescription(),
                i.getQuantity(),
                i.getUnit(),
                i.getNetUnitPrice(),
                i.getVatRate(),
                i.getNetTotal(),
                i.getVatAmount(),
                i.getGrossTotal()
        );
    }
}