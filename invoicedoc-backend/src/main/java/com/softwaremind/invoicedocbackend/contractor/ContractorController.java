package com.softwaremind.invoicedocbackend.contractor;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.softwaremind.invoicedocbackend.contractor.dto.ContractorCreateRequest;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorResponse;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorUpdateRequest;

@RestController
@RequestMapping("/v1/api/contractors")
@RequiredArgsConstructor
public class ContractorController {

    private final ContractorService contractorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<ContractorResponse> list(@RequestParam(name = "q", required = false) String query) {
        return contractorService.list(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ContractorResponse get(@PathVariable Long id) {
        return contractorService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<ContractorResponse> create(@RequestBody ContractorCreateRequest req) {
        ContractorResponse created = contractorService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ContractorResponse update(@PathVariable Long id,
                                     @RequestBody ContractorUpdateRequest req) {
        return contractorService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
