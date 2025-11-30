package com.softwaremind.invoicedocbackend.importing;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.softwaremind.invoicedocbackend.importing.dto.InvoiceImportDto;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    @Mock
    private ImportService importService;

    @Mock
    private XmlMapper xmlMapper;

    @InjectMocks
    private ImportController controller;

    private static final Long SELLER_PROFILE_ID = 123L;

    @Test
    @DisplayName("importJson powinien delegować do ImportService.importFromDto i zwrócić 201 CREATED")
    void importJsonShouldDelegateAndReturnCreated() {
        InvoiceImportDto dto = mock(InvoiceImportDto.class);
        InvoiceResponse serviceResponse = mock(InvoiceResponse.class);

        when(importService.importFromDto(dto, SELLER_PROFILE_ID))
                .thenReturn(serviceResponse);

        ResponseEntity<InvoiceResponse> response =
                controller.importJson(SELLER_PROFILE_ID, dto);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isSameAs(serviceResponse)
        );

        verify(importService).importFromDto(dto, SELLER_PROFILE_ID);
        verifyNoMoreInteractions(importService, xmlMapper);
    }

    @Test
    @DisplayName("importXml powinien zdeserializować XML do DTO, wywołać ImportService i zwrócić 201 CREATED")
    void importXmlShouldDeserializeAndDelegate() throws Exception {
        String xmlBody = "<invoiceImportDto>...</invoiceImportDto>";
        InvoiceImportDto dto = mock(InvoiceImportDto.class);
        InvoiceResponse serviceResponse = mock(InvoiceResponse.class);

        when(xmlMapper.readValue(xmlBody, InvoiceImportDto.class))
                .thenReturn(dto);
        when(importService.importFromDto(dto, SELLER_PROFILE_ID))
                .thenReturn(serviceResponse);

        ResponseEntity<InvoiceResponse> response =
                controller.importXml(SELLER_PROFILE_ID, xmlBody);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isSameAs(serviceResponse)
        );

        verify(xmlMapper).readValue(xmlBody, InvoiceImportDto.class);
        verify(importService).importFromDto(dto, SELLER_PROFILE_ID);
        verifyNoMoreInteractions(importService, xmlMapper);
    }

    @Test
    @DisplayName("importXml powinien propagować wyjątek gdy XmlMapper rzuci błąd przy parsowaniu")
    void importXmlShouldPropagateExceptionFromXmlMapper() throws Exception {
        String xmlBody = "<badXml>";
        RuntimeException parseError = new RuntimeException("bad xml");

        when(xmlMapper.readValue(xmlBody, InvoiceImportDto.class))
                .thenThrow(parseError);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> controller.importXml(SELLER_PROFILE_ID, xmlBody)
        );
        assertThat(ex).isSameAs(parseError);

        verify(xmlMapper).readValue(xmlBody, InvoiceImportDto.class);
        verifyNoInteractions(importService);
    }

    @Test
    @DisplayName("importCsv powinien delegować do ImportService.importFromCsv i zwrócić 201 CREATED")
    void importCsvShouldDelegateAndReturnCreated() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        InvoiceResponse serviceResponse = mock(InvoiceResponse.class);

        when(importService.importFromCsv(file, SELLER_PROFILE_ID))
                .thenReturn(serviceResponse);

        ResponseEntity<InvoiceResponse> response =
                controller.importCsv(SELLER_PROFILE_ID, file);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isSameAs(serviceResponse)
        );

        verify(importService).importFromCsv(file, SELLER_PROFILE_ID);
        verifyNoMoreInteractions(importService);
        verifyNoInteractions(xmlMapper);
    }
}
