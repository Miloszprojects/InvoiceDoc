package com.softwaremind.invoicedocbackend.importing.dto;

import java.util.List;

public record InvoiceImportDto(
        ImportPartyDto seller,
        ImportPartyDto buyer,
        ImportInvoiceMetaDto invoice,
        List<ImportInvoiceItemDto> items,
        ImportExtraDto extra
) {}
