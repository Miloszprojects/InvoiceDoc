package com.softwaremind.invoicedocbackend.importing.dto;

import org.springframework.stereotype.Component;

import java.util.List;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.contractor.ContractorType;
import com.softwaremind.invoicedocbackend.invoice.PaymentMethod;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;

@Component
public class InvoiceImportMapper {

    public InvoiceCreateRequest toCreateRequest(InvoiceImportDto importDto,
                                                Long sellerProfileId,
                                                Long contractorId) {

        PaymentMethod pm = PaymentMethod.BANK_TRANSFER;
        if (importDto.invoice().paymentMethod() != null) {
            try {
                pm = PaymentMethod.valueOf(importDto.invoice().paymentMethod());
            } catch (IllegalArgumentException ignored) {}
        }

        List<InvoiceItemCreateRequest> items = importDto.items().stream()
                .map(i -> new InvoiceItemCreateRequest(
                        i.description(),
                        i.quantity(),
                        i.unit(),
                        i.netUnitPrice(),
                        i.vatRate()
                ))
                .toList();

        return new InvoiceCreateRequest(
                sellerProfileId,
                contractorId,
                importDto.buyer().name(),
                importDto.buyer().nip(),
                importDto.buyer().pesel(),
                importDto.invoice().issueDate(),
                importDto.invoice().saleDate(),
                importDto.invoice().dueDate(),
                pm,
                importDto.invoice().currency(),
                importDto.extra() != null ? importDto.extra().notes() : null,
                importDto.extra() != null && Boolean.TRUE.equals(importDto.extra().reverseCharge()),
                importDto.extra() != null && Boolean.TRUE.equals(importDto.extra().splitPayment()),
                items
        );
    }

    public ContractorType mapBuyerType(ImportPartyDto buyer) {
        if (buyer == null || buyer.type() == null) return ContractorType.COMPANY;
        return switch (buyer.type()) {
            case "COMPANY" -> ContractorType.COMPANY;
            case "PERSON" -> ContractorType.PERSON;
            default -> ContractorType.COMPANY;
        };
    }

    public AddressDto toAddressDto(ImportPartyDto party) {
        if (party == null || party.address() == null) return null;
        return party.address();
    }
}
