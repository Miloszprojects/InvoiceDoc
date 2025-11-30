package com.softwaremind.invoicedocbackend.tenant;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.softwaremind.invoicedocbackend.invoice.InvoiceRepository;
import com.softwaremind.invoicedocbackend.security.CurrentUser;
import com.softwaremind.invoicedocbackend.security.CurrentUserProvider;
import com.softwaremind.invoicedocbackend.security.UserRole;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileCreateRequest;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileResponse;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileUpdateRequest;
import com.softwaremind.invoicedocbackend.tenant.mapper.SellerProfileMapper;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final OrganizationRepository organizationRepository;
    private final SellerProfileMapper mapper;
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

    public List<SellerProfileResponse> getMyProfiles() {
        CurrentUser cu = currentUserProvider.getCurrentUser();
        List<SellerProfileEntity> list =
                sellerProfileRepository.findByOrganizationId(cu.organizationId());
        return list.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public SellerProfileResponse createMyProfile(SellerProfileCreateRequest req) {
        assertCanModify();

        CurrentUser cu = currentUserProvider.getCurrentUser();
        OrganizationEntity org = organizationRepository.findById(cu.organizationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ORG_NOT_FOUND"));

        SellerProfileEntity entity = mapper.fromCreateRequest(req, org);
        SellerProfileEntity saved = sellerProfileRepository.save(entity);
        return mapper.toResponse(saved);
    }

    @Transactional
    public SellerProfileResponse updateMyProfile(Long id, SellerProfileUpdateRequest req) {
        assertCanModify();

        CurrentUser cu = currentUserProvider.getCurrentUser();

        SellerProfileEntity entity = sellerProfileRepository
                .findByOrganizationIdAndId(cu.organizationId(), id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SELLER_PROFILE_NOT_FOUND"));

        mapper.updateEntity(entity, req);
        SellerProfileEntity saved = sellerProfileRepository.save(entity);

        return mapper.toResponse(saved);
    }

    @Transactional
    public void deleteMyProfile(Long id) {
        assertCanModify();

        CurrentUser cu = currentUserProvider.getCurrentUser();

        SellerProfileEntity profile = sellerProfileRepository
                .findByOrganizationIdAndId(cu.organizationId(), id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "SELLER_PROFILE_NOT_FOUND"));

        invoiceRepository.clearSellerProfileForInvoices(profile.getId());
        sellerProfileRepository.delete(profile);
    }
}
