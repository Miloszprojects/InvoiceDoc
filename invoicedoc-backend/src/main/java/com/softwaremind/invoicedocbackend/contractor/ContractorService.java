package com.softwaremind.invoicedocbackend.contractor;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import com.softwaremind.invoicedocbackend.contractor.dto.ContractorCreateRequest;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorResponse;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorUpdateRequest;
import com.softwaremind.invoicedocbackend.contractor.mapper.ContractorMapper;
import com.softwaremind.invoicedocbackend.invoice.InvoiceRepository;
import com.softwaremind.invoicedocbackend.security.CurrentUser;
import com.softwaremind.invoicedocbackend.security.CurrentUserProvider;
import com.softwaremind.invoicedocbackend.security.UserRole;
import com.softwaremind.invoicedocbackend.tenant.OrganizationEntity;
import com.softwaremind.invoicedocbackend.tenant.OrganizationRepository;

@Service
@RequiredArgsConstructor
public class ContractorService {

    private final ContractorRepository contractorRepository;
    private final OrganizationRepository organizationRepository;
    private final ContractorMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final InvoiceRepository invoiceRepository;

    private void assertCanModify() {
        CurrentUser cu = currentUserProvider.getCurrentUser();
        UserRole role = cu.role();

        if (role != UserRole.ADMIN && role != UserRole.OWNER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "ONLY_OWNER_OR_ADMIN"
            );
        }
    }

    public List<ContractorResponse> list(String search) {
        CurrentUser cu = currentUserProvider.getCurrentUser();

        List<ContractorEntity> entities =
                (search != null && !search.isBlank())
                        ? contractorRepository.findByOrganizationIdAndNameContainingIgnoreCase(
                        cu.organizationId(), search.trim())
                        : contractorRepository.findByOrganizationId(cu.organizationId());

        return entities.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ContractorResponse get(Long id) {
        CurrentUser cu = currentUserProvider.getCurrentUser();

        ContractorEntity entity = contractorRepository
                .findByOrganizationIdAndId(cu.organizationId(), id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CONTRACTOR_NOT_FOUND"));

        return mapper.toResponse(entity);
    }

    @Transactional
    public ContractorResponse create(ContractorCreateRequest req) {
        assertCanModify();

        CurrentUser cu = currentUserProvider.getCurrentUser();

        OrganizationEntity org = organizationRepository.findById(cu.organizationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ORG_NOT_FOUND"));

        ContractorEntity entity = mapper.fromCreateRequest(req, org);
        ContractorEntity saved = contractorRepository.save(entity);

        return mapper.toResponse(saved);
    }

    @Transactional
    public ContractorResponse update(Long id, ContractorUpdateRequest req) {
        assertCanModify();

        CurrentUser cu = currentUserProvider.getCurrentUser();

        ContractorEntity entity = contractorRepository
                .findByOrganizationIdAndId(cu.organizationId(), id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CONTRACTOR_NOT_FOUND"));

        mapper.updateEntity(entity, req);

        ContractorEntity saved = contractorRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        assertCanModify();

        CurrentUser cu = currentUserProvider.getCurrentUser();

        ContractorEntity entity = contractorRepository
                .findByOrganizationIdAndId(cu.organizationId(), id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CONTRACTOR_NOT_FOUND"));

        invoiceRepository.clearContractorForInvoices(entity.getId());
        contractorRepository.delete(entity);
    }
}
