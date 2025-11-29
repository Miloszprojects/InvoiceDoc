package com.softwaremind.invoicedocbackend.importing;

import java.io.IOException;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import com.softwaremind.invoicedocbackend.importing.dto.InvoiceImportDto;
import com.softwaremind.invoicedocbackend.invoice.dto.InvoiceResponse;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;
    private final XmlMapper xmlMapper;


    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<InvoiceResponse> importJson(
            @RequestParam("sellerProfileId") Long sellerProfileId,
            @RequestBody InvoiceImportDto dto
    ) {
        InvoiceResponse response = importService.importFromDto(dto, sellerProfileId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping(value = "/xml", consumes = MediaType.APPLICATION_XML_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<InvoiceResponse> importXml(
            @RequestParam("sellerProfileId") Long sellerProfileId,
            @RequestBody String xmlBody
    ) throws IOException {
        InvoiceImportDto dto = xmlMapper.readValue(xmlBody, InvoiceImportDto.class);
        InvoiceResponse response = importService.importFromDto(dto, sellerProfileId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<InvoiceResponse> importCsv(
            @RequestParam("sellerProfileId") Long sellerProfileId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        InvoiceResponse response = importService.importFromCsv(file, sellerProfileId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
