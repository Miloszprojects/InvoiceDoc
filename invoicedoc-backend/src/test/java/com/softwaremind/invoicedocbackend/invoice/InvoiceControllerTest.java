package com.softwaremind.invoicedocbackend.invoice;

import com.softwaremind.invoicedocbackend.common.dto.AddressDto;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemCreateRequest;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceItemResponse;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import com.softwaremind.invoicedocbackend.invoice.pdf.InvoicePdfService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.softwaremind.invoicedocbackend.invoice.InvoiceStatus.DRAFT;
import static com.softwaremind.invoicedocbackend.invoice.PaymentMethod.BANK_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoicePdfService invoicePdfService;

    @InjectMocks
    private InvoiceController controller;

    private InvoiceCreateRequest sampleCreateRequest() {
        InvoiceItemCreateRequest itemReq = new InvoiceItemCreateRequest(
                "Item 1",
                new BigDecimal("2.00"),
                "pcs",
                new BigDecimal("100.00"),
                "23"
        );

        return new InvoiceCreateRequest(
                10L,
                20L,
                "Buyer Name Override",
                "1234567890",
                "90010112345",
                LocalDate.of(2024, 1, 5),
                LocalDate.of(2024, 1, 5),
                LocalDate.of(2024, 1, 20),
                BANK_TRANSFER,
                "PLN",
                "Some notes",
                false,
                false,
                List.of(itemReq)
        );
    }

    private InvoiceResponse sampleInvoiceResponse() {
        AddressDto sellerAddr = new AddressDto(
                "Seller St",
                "1A",
                null,
                "00-001",
                "Warsaw",
                "PL"
        );
        AddressDto buyerAddr = new AddressDto(
                "Buyer St",
                "2B",
                "3",
                "00-002",
                "Cracow",
                "PL"
        );

        InvoiceItemResponse item = new InvoiceItemResponse(
                100L,
                "Item 1",
                new BigDecimal("2.00"),
                "pcs",
                new BigDecimal("100.00"),
                "23",
                new BigDecimal("200.00"),
                new BigDecimal("46.00"),
                new BigDecimal("246.00")
        );

        return new InvoiceResponse(
                1L,
                "FV/2024/01/05/001",
                DRAFT,
                LocalDate.of(2024, 1, 5),
                LocalDate.of(2024, 1, 5),
                LocalDate.of(2024, 1, 20),
                BANK_TRANSFER,
                "PLN",
                "Seller Sp. z o.o.",
                "1234567890",
                sellerAddr,
                "PL00123456789000000000000000",
                "Buyer Sp. z o.o.",
                "9876543210",
                buyerAddr,
                "Some notes",
                false,
                false,
                new BigDecimal("200.00"),
                new BigDecimal("46.00"),
                new BigDecimal("246.00"),
                List.of(item)
        );
    }

    @Test
    @DisplayName("create should delegate to service and return 201 CREATED with body")
    void createShouldDelegateToServiceAndReturnCreated() {
        InvoiceCreateRequest req = sampleCreateRequest();
        InvoiceResponse serviceResp = sampleInvoiceResponse();

        when(invoiceService.createInvoice(req)).thenReturn(serviceResp);

        ResponseEntity<InvoiceResponse> response = controller.create(req);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isEqualTo(serviceResp)
        );

        ArgumentCaptor<InvoiceCreateRequest> captor =
                ArgumentCaptor.forClass(InvoiceCreateRequest.class);

        verify(invoiceService).createInvoice(captor.capture());
        verifyNoMoreInteractions(invoiceService, invoicePdfService);

        InvoiceCreateRequest passed = captor.getValue();
        assertThat(passed.sellerProfileId()).isEqualTo(req.sellerProfileId());
        assertThat(passed.contractorId()).isEqualTo(req.contractorId());
        assertThat(passed.items()).hasSize(1);
    }

    @Test
    @DisplayName("list should delegate to service with parameters and return Page from service")
    void listShouldReturnPageFromService() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        int page = 0;
        int size = 20;

        InvoiceResponse resp = sampleInvoiceResponse();
        Page<InvoiceResponse> pageResp = new PageImpl<>(List.of(resp));

        when(invoiceService.listInvoices(from, to, page, size)).thenReturn(pageResp);

        Page<InvoiceResponse> result = controller.list(from, to, page, size);

        assertAll(
                () -> assertThat(result.getContent()).hasSize(1),
                () -> assertThat(result.getContent().get(0)).isEqualTo(resp)
        );

        verify(invoiceService).listInvoices(from, to, page, size);
        verifyNoMoreInteractions(invoiceService, invoicePdfService);
    }

    @Test
    @DisplayName("list without from/to should pass nulls to service")
    void listWithoutDatesShouldPassNulls() {
        int page = 2;
        int size = 5;

        InvoiceResponse resp = sampleInvoiceResponse();
        Page<InvoiceResponse> pageResp = new PageImpl<>(List.of(resp));

        when(invoiceService.listInvoices(null, null, page, size)).thenReturn(pageResp);

        Page<InvoiceResponse> result = controller.list(null, null, page, size);

        assertThat(result.getContent()).containsExactly(resp);

        verify(invoiceService).listInvoices(null, null, page, size);
        verifyNoMoreInteractions(invoiceService, invoicePdfService);
    }

    @Test
    @DisplayName("get should delegate to service and return InvoiceResponse")
    void getShouldReturnInvoiceFromService() {
        long id = 123L;
        InvoiceResponse resp = sampleInvoiceResponse();

        when(invoiceService.getInvoice(id)).thenReturn(resp);

        InvoiceResponse result = controller.get(id);

        assertThat(result).isEqualTo(resp);

        verify(invoiceService).getInvoice(id);
        verifyNoMoreInteractions(invoiceService, invoicePdfService);
    }

    @Test
    @DisplayName("pdf should ask service for entity, generate PDF and set headers")
    void pdfShouldGeneratePdfAndSetHeaders() {
        long id = 42L;

        InvoiceEntity entity = new InvoiceEntity();
        entity.setId(id);
        entity.setNumber("FV 2024/01/05 001");

        byte[] pdfBytes = "PDF-DATA".getBytes();

        when(invoiceService.getInvoiceEntityForPdf(id)).thenReturn(entity);
        when(invoicePdfService.generateInvoicePdf(entity)).thenReturn(pdfBytes);

        ResponseEntity<byte[]> response = controller.pdf(id);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isEqualTo(pdfBytes),
                () -> assertThat(response.getHeaders().getContentType())
                        .isEqualTo(MediaType.APPLICATION_PDF),
                () -> assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                        .contains("attachment")
                        .contains("invoice-FV_2024/01/05_001.pdf")
        );

        verify(invoiceService).getInvoiceEntityForPdf(id);
        verify(invoicePdfService).generateInvoicePdf(entity);
        verifyNoMoreInteractions(invoiceService, invoicePdfService);
    }

    @Test
    @DisplayName("delete should delegate to service and return 204 NO_CONTENT")
    void deleteShouldCallServiceAndReturnNoContent() {
        long id = 77L;

        ResponseEntity<Void> response = controller.delete(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(invoiceService).deleteInvoice(id);
        verifyNoMoreInteractions(invoiceService, invoicePdfService);
    }
}
