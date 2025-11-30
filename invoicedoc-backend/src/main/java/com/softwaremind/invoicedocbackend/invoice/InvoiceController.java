package com.softwaremind.invoicedocbackend.invoice;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.invoice.pdf.InvoicePdfService;

@RestController
@RequestMapping("/v1/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<InvoiceResponse> create(@RequestBody InvoiceCreateRequest req) {
        InvoiceResponse created = invoiceService.createInvoice(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ACCOUNTANT','ADMIN')")
    public Page<InvoiceResponse> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return invoiceService.listInvoices(from, to, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ACCOUNTANT','ADMIN')")
    public InvoiceResponse get(@PathVariable Long id) {
        return invoiceService.getInvoice(id);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('OWNER','ACCOUNTANT','ADMIN')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        InvoiceEntity invoice = invoiceService.getInvoiceEntityForPdf(id);
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(("invoice-" + invoice.getNumber() + ".pdf").replace(" ", "_"))
                        .build()
        );

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
