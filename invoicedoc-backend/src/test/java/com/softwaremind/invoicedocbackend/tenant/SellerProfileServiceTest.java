package com.softwaremind.invoicedocbackend.tenant;

import com.softwaremind.invoicedocbackend.invoice.InvoiceRepository;
import com.softwaremind.invoicedocbackend.security.CurrentUser;
import com.softwaremind.invoicedocbackend.security.CurrentUserProvider;
import com.softwaremind.invoicedocbackend.security.UserRole;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileCreateRequest;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileResponse;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileUpdateRequest;
import com.softwaremind.invoicedocbackend.tenant.mapper.SellerProfileMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerProfileServiceTest {

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SellerProfileMapper mapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private SellerProfileService service;

    private static final Long ORG_ID = 100L;
    private static final Long PROFILE_ID = 200L;

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
    @DisplayName("getMyProfiles should return mapped profiles for current user's organization")
    void getMyProfilesShouldReturnMappedProfiles() {
        CurrentUser cu = mockUserWithOrgOnly(ORG_ID);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileEntity e1 = SellerProfileEntity.builder().id(1L).build();
        SellerProfileEntity e2 = SellerProfileEntity.builder().id(2L).build();

        SellerProfileResponse r1 = new SellerProfileResponse(1L, "Seller1", null, null, null, null, null, null, null, null, null);
        SellerProfileResponse r2 = new SellerProfileResponse(2L, "Seller2", null, null, null, null, null, null, null, null, null);

        when(sellerProfileRepository.findByOrganizationId(ORG_ID))
                .thenReturn(List.of(e1, e2));
        when(mapper.toResponse(e1)).thenReturn(r1);
        when(mapper.toResponse(e2)).thenReturn(r2);

        List<SellerProfileResponse> result = service.getMyProfiles();

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0)).isSameAs(r1),
                () -> assertThat(result.get(1)).isSameAs(r2)
        );

        verify(currentUserProvider).getCurrentUser();
        verify(sellerProfileRepository).findByOrganizationId(ORG_ID);
        verify(mapper).toResponse(e1);
        verify(mapper).toResponse(e2);
    }

    @Test
    @DisplayName("createMyProfile should create profile for current org when user is ADMIN")
    void createMyProfileShouldCreateProfileForAdmin() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileCreateRequest req = new SellerProfileCreateRequest(
                "Seller",
                "1234567890",
                "987654321",
                "0000123456",
                "Bank",
                "PL00123456789000000000000000",
                null,
                "PLN",
                14
        );

        OrganizationEntity org = OrganizationEntity.builder()
                .id(ORG_ID)
                .name("Org")
                .build();

        SellerProfileEntity entity = SellerProfileEntity.builder()
                .name("Seller")
                .organization(org)
                .build();

        SellerProfileEntity saved = SellerProfileEntity.builder()
                .id(PROFILE_ID)
                .name("Seller")
                .organization(org)
                .build();

        SellerProfileResponse response = new SellerProfileResponse(
                PROFILE_ID,
                "Seller",
                "1234567890",
                "987654321",
                "0000123456",
                "Bank",
                "PL00123456789000000000000000",
                null,
                "PLN",
                14,
                null
        );

        when(organizationRepository.findById(ORG_ID))
                .thenReturn(Optional.of(org));
        when(mapper.fromCreateRequest(req, org)).thenReturn(entity);
        when(sellerProfileRepository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        SellerProfileResponse result = service.createMyProfile(req);

        assertThat(result).isSameAs(response);

        verify(currentUserProvider, times(2)).getCurrentUser(); // assertCanModify + method body
        verify(organizationRepository).findById(ORG_ID);
        verify(mapper).fromCreateRequest(req, org);
        verify(sellerProfileRepository).save(entity);
        verify(mapper).toResponse(saved);
    }

    @Test
    @DisplayName("createMyProfile should throw FORBIDDEN when user is not allowed to modify (role null)")
    void createMyProfileShouldThrowForbiddenWhenUserNotAllowed() {
        CurrentUser cu = mockUserWithRoleOnly(null);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileCreateRequest req = mock(SellerProfileCreateRequest.class);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createMyProfile(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_OWNER_OR_ADMIN")
        );

        verify(organizationRepository, never()).findById(anyLong());
        verify(sellerProfileRepository, never()).save(any());
        verify(mapper, never()).fromCreateRequest(any(), any());
    }

    @Test
    @DisplayName("createMyProfile should throw NOT_FOUND when organization does not exist")
    void createMyProfileShouldThrowNotFoundWhenOrgNotFound() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileCreateRequest req = mock(SellerProfileCreateRequest.class);

        when(organizationRepository.findById(ORG_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createMyProfile(req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ORG_NOT_FOUND")
        );

        verify(organizationRepository).findById(ORG_ID);
        verify(mapper, never()).fromCreateRequest(any(), any());
        verify(sellerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMyProfile should update entity and return mapped response when profile exists")
    void updateMyProfileShouldUpdateWhenExists() {
        // given
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileUpdateRequest req = new SellerProfileUpdateRequest(
                "Updated Name",
                "1234567890",
                "987654321",
                "0000123456",
                "New Bank",
                "PL00123456789000000000000099",
                null,
                "EUR",
                21,
                "INV-{YYYY}/{NNN}"
        );

        SellerProfileEntity entity = SellerProfileEntity.builder()
                .id(PROFILE_ID)
                .name("Old Name")
                .build();

        SellerProfileEntity saved = SellerProfileEntity.builder()
                .id(PROFILE_ID)
                .name("Updated Name")
                .build();

        SellerProfileResponse response = new SellerProfileResponse(
                PROFILE_ID,
                "Updated Name",
                "1234567890",
                "987654321",
                "0000123456",
                "New Bank",
                "PL00123456789000000000000099",
                null,
                "EUR",
                21,
                null
        );

        when(sellerProfileRepository.findByOrganizationIdAndId(ORG_ID, PROFILE_ID))
                .thenReturn(Optional.of(entity));
        doNothing().when(mapper).updateEntity(entity, req);
        when(sellerProfileRepository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);

        SellerProfileResponse result = service.updateMyProfile(PROFILE_ID, req);

        assertThat(result).isSameAs(response);

        verify(currentUserProvider, times(2)).getCurrentUser(); // assertCanModify + method body
        verify(sellerProfileRepository).findByOrganizationIdAndId(ORG_ID, PROFILE_ID);
        verify(mapper).updateEntity(entity, req);
        verify(sellerProfileRepository).save(entity);
        verify(mapper).toResponse(saved);
    }

    @Test
    @DisplayName("updateMyProfile should throw FORBIDDEN when user is not allowed to modify")
    void updateMyProfileShouldThrowForbiddenWhenUserNotAllowed() {
        CurrentUser cu = mockUserWithRoleOnly(null); // role null => forbidden
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileUpdateRequest req = mock(SellerProfileUpdateRequest.class);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.updateMyProfile(PROFILE_ID, req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_OWNER_OR_ADMIN")
        );

        verify(sellerProfileRepository, never()).findByOrganizationIdAndId(anyLong(), anyLong());
        verify(mapper, never()).updateEntity(any(), any());
        verify(sellerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMyProfile should throw NOT_FOUND when profile does not exist in current organization")
    void updateMyProfileShouldThrowNotFoundWhenProfileNotFound() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileUpdateRequest req = mock(SellerProfileUpdateRequest.class);

        when(sellerProfileRepository.findByOrganizationIdAndId(ORG_ID, PROFILE_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.updateMyProfile(PROFILE_ID, req)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(ex.getReason()).isEqualTo("SELLER_PROFILE_NOT_FOUND")
        );

        verify(sellerProfileRepository).findByOrganizationIdAndId(ORG_ID, PROFILE_ID);
        verify(mapper, never()).updateEntity(any(), any());
        verify(sellerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteMyProfile should clear seller profile from invoices and delete profile when exists")
    void deleteMyProfileShouldClearInvoicesAndDeleteWhenExists() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.ADMIN);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        SellerProfileEntity profile = SellerProfileEntity.builder()
                .id(PROFILE_ID)
                .name("Seller")
                .build();

        when(sellerProfileRepository.findByOrganizationIdAndId(ORG_ID, PROFILE_ID))
                .thenReturn(Optional.of(profile));

        service.deleteMyProfile(PROFILE_ID);

        verify(currentUserProvider, times(2)).getCurrentUser();
        verify(sellerProfileRepository).findByOrganizationIdAndId(ORG_ID, PROFILE_ID);
        verify(invoiceRepository).clearSellerProfileForInvoices(PROFILE_ID);
        verify(sellerProfileRepository).delete(profile);
    }

    @Test
    @DisplayName("deleteMyProfile should throw FORBIDDEN when user is not allowed to modify")
    void deleteMyProfileShouldThrowForbiddenWhenUserNotAllowed() {
        CurrentUser cu = mockUserWithRoleOnly(null);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteMyProfile(PROFILE_ID)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value()),
                () -> assertThat(ex.getReason()).isEqualTo("ONLY_OWNER_OR_ADMIN")
        );

        verify(sellerProfileRepository, never()).findByOrganizationIdAndId(anyLong(), anyLong());
        verify(invoiceRepository, never()).clearSellerProfileForInvoices(anyLong());
        verify(sellerProfileRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteMyProfile should throw NOT_FOUND when profile does not exist in current organization")
    void deleteMyProfileShouldThrowNotFoundWhenProfileNotFound() {
        CurrentUser cu = mockUserWithOrgAndRole(ORG_ID, UserRole.OWNER);
        when(currentUserProvider.getCurrentUser()).thenReturn(cu);

        when(sellerProfileRepository.findByOrganizationIdAndId(ORG_ID, PROFILE_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteMyProfile(PROFILE_ID)
        );

        assertAll(
                () -> assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()),
                () -> assertThat(ex.getReason()).isEqualTo("SELLER_PROFILE_NOT_FOUND")
        );

        verify(sellerProfileRepository).findByOrganizationIdAndId(ORG_ID, PROFILE_ID);
        verify(invoiceRepository, never()).clearSellerProfileForInvoices(anyLong());
        verify(sellerProfileRepository, never()).delete(any());
    }
}
