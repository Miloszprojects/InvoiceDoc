package com.softwaremind.invoicedocbackend.contractor;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractorServiceTest {

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ContractorMapper mapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private ContractorService service;

    private static final Long ORG_ID = 100L;
    private static final Long CONTRACTOR_ID = 200L;

    private CurrentUser mockUserWithOrgOnly(Long orgId) {
        CurrentUser cu = mock(CurrentUser.class);
        when(cu.organizationId()).thenReturn(orgId);
        return cu;
    }

    private CurrentUser mockUserWithOrgAndRole(Long orgId, UserRole role) {
        CurrentUser cu = mock(CurrentUser.class);
        when(cu.organizationId()).thenReturn(orgId);
        when(cu.role()).thenReturn(role);
        return cu;
    }

    private CurrentUser mockUserWithRoleOnly(UserRole role) {
        CurrentUser cu = mock(CurrentUser.class);
        when(cu.role()).thenReturn(role);
        return cu;
    }

    @Test
    @DisplayName("list should return mapped contractors for current org when search is null")
    void listShouldReturnMappedContractorsWhenSearchNull() {
        CurrentUser cu = mockUserWithOrgOnly(ORG_ID);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorEntity e1 = ContractorEntity.builder().id(1L).name("A").build();
        ContractorEntity e2 = ContractorEntity.builder().id(2L).name("B").build();

        ContractorResponse r1 = new ContractorResponse(1L, null, "A", null, null, null, null, null, false);
        ContractorResponse r2 = new ContractorResponse(2L, null, "B", null, null, null, null, null, true);

        when(contractorRepository.findByOrganizationId(ORG_ID))
                .thenReturn(List.of(e1, e2));
        when(mapper.toResponse(e1)).thenReturn(r1);
        when(mapper.toResponse(e2)).thenReturn(r2);

        List<ContractorResponse> result = service.list(null);

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0)).isSameAs(r1),
                () -> assertThat(result.get(1)).isSameAs(r2)
        );

        verify(currentUserProvider).getCurrentUser();
        verify(contractorRepository).findByOrganizationId(ORG_ID);
        verify(contractorRepository, never())
                .findByOrganizationIdAndNameContainingIgnoreCase(anyLong(), anyString());
    }

    @Test
    @DisplayName("list should return mapped contractors for current org when search is blank")
    void listShouldReturnMappedContractorsWhenSearchBlank() {
        CurrentUser cu = mockUserWithOrgOnly(ORG_ID);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorEntity e = ContractorEntity.builder().id(1L).name("A").build();
        ContractorResponse r = new ContractorResponse(1L, null, "A", null, null, null, null, null, false);

        when(contractorRepository.findByOrganizationId(ORG_ID))
                .thenReturn(List.of(e));
        when(mapper.toResponse(e)).thenReturn(r);

        List<ContractorResponse> result = service.list("   ");

        assertAll(
                () -> assertThat(result).hasSize(1),
                () -> assertThat(result.get(0)).isSameAs(r)
        );

        verify(currentUserProvider).getCurrentUser();
        verify(contractorRepository).findByOrganizationId(ORG_ID);
        verify(contractorRepository, never())
                .findByOrganizationIdAndNameContainingIgnoreCase(anyLong(), anyString());
    }

    @Test
    @DisplayName("list should use search and trim it when not blank")
    void listShouldFilterBySearchWhenNotBlank() {
        CurrentUser cu = mockUserWithOrgOnly(ORG_ID);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorEntity e1 = ContractorEntity.builder().id(1L).name("ACME").build();
        ContractorEntity e2 = ContractorEntity.builder().id(2L).name("Acme Partner").build();

        ContractorResponse r1 = new ContractorResponse(1L, null, "ACME", null, null, null, null, null, false);
        ContractorResponse r2 = new ContractorResponse(2L, null, "Acme Partner", null, null, null, null, null, true);

        when(contractorRepository.findByOrganizationIdAndNameContainingIgnoreCase(ORG_ID, "acme"))
                .thenReturn(List.of(e1, e2));
        when(mapper.toResponse(e1)).thenReturn(r1);
        when(mapper.toResponse(e2)).thenReturn(r2);

        List<ContractorResponse> result = service.list("  acme   ");

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0)).isSameAs(r1),
                () -> assertThat(result.get(1)).isSameAs(r2)
        );

        verify(currentUserProvider).getCurrentUser();
        verify(contractorRepository)
                .findByOrganizationIdAndNameContainingIgnoreCase(ORG_ID, "acme");
        verify(contractorRepository, never()).findByOrganizationId(anyLong());
    }

    @Test
    @DisplayName("get should return mapped contractor when found in current org")
    void getShouldReturnMappedContractorWhenFound() {
        CurrentUser cu = mockUserWithOrgOnly(ORG_ID);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorEntity entity = ContractorEntity.builder()
                .id(CONTRACTOR_ID)
                .name("ACME")
                .build();

        ContractorResponse response = new ContractorResponse(
                CONTRACTOR_ID,
                null,
                "ACME",
                null,
                null,
                null,
                null,
                null,
                true
        );

        when(contractorRepository.findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID))
                .thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(response);

        ContractorResponse result = service.get(CONTRACTOR_ID);

        assertThat(result).isSameAs(response);

        verify(currentUserProvider).getCurrentUser();
        verify(contractorRepository).findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID);
        verify(mapper).toResponse(entity);
    }

    @Test
    @DisplayName("get should throw NOT_FOUND when contractor not found in current org")
    void getShouldThrowNotFoundWhenContractorNotFound() {
        CurrentUser cu = mockUserWithOrgOnly(ORG_ID);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        when(contractorRepository.findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.get(CONTRACTOR_ID)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(ex.getReason()).isEqualTo("CONTRACTOR_NOT_FOUND")
        );

        verify(contractorRepository).findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID);
        verify(mapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("create should create contractor for current org when user is ADMIN")
    void createShouldCreateContractorForAdmin() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorCreateRequest req = new ContractorCreateRequest(
                ContractorType.COMPANY,
                "ACME",
                "1234567890",
                null,
                null,
                "acme@example.com",
                "+48 123 456 789",
                true
        );

        OrganizationEntity org = OrganizationEntity.builder()
                .id(ORG_ID)
                .name("Org")
                .build();

        ContractorEntity entity = ContractorEntity.builder()
                .name("ACME")
                .organization(org)
                .build();

        ContractorEntity saved = ContractorEntity.builder()
                .id(CONTRACTOR_ID)
                .name("ACME")
                .organization(org)
                .build();

        ContractorResponse response = new ContractorResponse(
                CONTRACTOR_ID,
                ContractorType.COMPANY,
                "ACME",
                "1234567890",
                null,
                null,
                "acme@example.com",
                "+48 123 456 789",
                true
        );

        when(organizationRepository.findById(ORG_ID))
                .thenReturn(Optional.of(org));
        when(mapper.fromCreateRequest(req, org)).thenReturn(entity);
        when(contractorRepository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        ContractorResponse result = service.create(req);

        assertThat(result).isSameAs(response);

        verify(currentUserProvider, times(2)).getCurrentUser(); // assertCanModify + body
        verify(organizationRepository).findById(ORG_ID);
        verify(mapper).fromCreateRequest(req, org);
        verify(contractorRepository).save(entity);
        verify(mapper).toResponse(saved);
    }

    @Test
    @DisplayName("create should throw FORBIDDEN when user is not allowed to modify")
    void createShouldThrowForbiddenWhenUserNotAllowed() {
        CurrentUser cu = mockUserWithRoleOnly(null);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorCreateRequest req = mock(ContractorCreateRequest.class);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_OWNER_OR_ADMIN")
        );

        verify(organizationRepository, never()).findById(anyLong());
        verify(contractorRepository, never()).save(any());
        verify(mapper, never()).fromCreateRequest(any(), any());
    }

    @Test
    @DisplayName("create should throw NOT_FOUND when organization does not exist")
    void createShouldThrowNotFoundWhenOrgNotFound() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorCreateRequest req = mock(ContractorCreateRequest.class);

        when(organizationRepository.findById(ORG_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ORG_NOT_FOUND")
        );

        verify(organizationRepository).findById(ORG_ID);
        verify(mapper, never()).fromCreateRequest(any(), any());
        verify(contractorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should update contractor and return mapped response when found")
    void updateShouldUpdateWhenFound() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorUpdateRequest req = new ContractorUpdateRequest(
                ContractorType.PERSON,
                "John Doe",
                null,
                "90010112345",
                null,
                "john.doe@example.com",
                "+48 987 654 321",
                false
        );

        ContractorEntity entity = ContractorEntity.builder()
                .id(CONTRACTOR_ID)
                .name("Old Name")
                .build();

        ContractorEntity saved = ContractorEntity.builder()
                .id(CONTRACTOR_ID)
                .name("John Doe")
                .build();

        ContractorResponse response = new ContractorResponse(
                CONTRACTOR_ID,
                ContractorType.PERSON,
                "John Doe",
                null,
                "90010112345",
                null,
                "john.doe@example.com",
                "+48 987 654 321",
                false
        );

        when(contractorRepository.findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID))
                .thenReturn(Optional.of(entity));
        doNothing().when(mapper).updateEntity(entity, req);
        when(contractorRepository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        ContractorResponse result = service.update(CONTRACTOR_ID, req);

        assertThat(result).isSameAs(response);

        verify(currentUserProvider, times(2)).getCurrentUser(); // assertCanModify + body
        verify(contractorRepository).findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID);
        verify(mapper).updateEntity(entity, req);
        verify(contractorRepository).save(entity);
        verify(mapper).toResponse(saved);
    }

    @Test
    @DisplayName("update should throw FORBIDDEN when user is not allowed to modify")
    void updateShouldThrowForbiddenWhenUserNotAllowed() {
        CurrentUser cu = mockUserWithRoleOnly(null);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorUpdateRequest req = mock(ContractorUpdateRequest.class);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.update(CONTRACTOR_ID, req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_OWNER_OR_ADMIN")
        );

        verify(contractorRepository, never()).findByOrganizationIdAndId(anyLong(), anyLong());
        verify(mapper, never()).updateEntity(any(), any());
        verify(contractorRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw NOT_FOUND when contractor does not exist in current org")
    void updateShouldThrowNotFoundWhenContractorNotFound() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorUpdateRequest req = mock(ContractorUpdateRequest.class);

        when(contractorRepository.findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.update(CONTRACTOR_ID, req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(ex.getReason()).isEqualTo("CONTRACTOR_NOT_FOUND")
        );

        verify(contractorRepository).findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID);
        verify(mapper, never()).updateEntity(any(), any());
        verify(contractorRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete should clear contractor from invoices and delete when exists")
    void deleteShouldClearInvoicesAndDeleteWhenExists() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ContractorEntity entity = ContractorEntity.builder()
                .id(CONTRACTOR_ID)
                .name("ACME")
                .build();

        when(contractorRepository.findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID))
                .thenReturn(Optional.of(entity));

        service.delete(CONTRACTOR_ID);

        verify(currentUserProvider, times(2)).getCurrentUser(); // assertCanModify + body
        verify(contractorRepository).findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID);
        verify(invoiceRepository).clearContractorForInvoices(CONTRACTOR_ID);
        verify(contractorRepository).delete(entity);
    }

    @Test
    @DisplayName("delete should throw FORBIDDEN when user is not allowed to modify")
    void deleteShouldThrowForbiddenWhenUserNotAllowed() {
        CurrentUser cu = mockUserWithRoleOnly(null);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(CONTRACTOR_ID)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_OWNER_OR_ADMIN")
        );

        verify(contractorRepository, never()).findByOrganizationIdAndId(anyLong(), anyLong());
        verify(invoiceRepository, never()).clearContractorForInvoices(anyLong());
        verify(contractorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete should throw NOT_FOUND when contractor does not exist in current org")
    void deleteShouldThrowNotFoundWhenContractorNotFound() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        when(contractorRepository.findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(CONTRACTOR_ID)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(ex.getReason()).isEqualTo("CONTRACTOR_NOT_FOUND")
        );

        verify(contractorRepository).findByOrganizationIdAndId(ORG_ID, CONTRACTOR_ID);
        verify(invoiceRepository, never()).clearContractorForInvoices(anyLong());
        verify(contractorRepository, never()).delete(any());
    }
}
