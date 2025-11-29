package com.softwaremind.invoicedocbackend.tenant;

import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileCreateRequest;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileResponse;
import com.softwaremind.invoicedocbackend.tenant.dto.SellerProfileUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerProfileControllerTest {

    @Mock
    private SellerProfileService sellerProfileService;

    @InjectMocks
    private SellerProfileController controller;


    @Test
    @DisplayName("list should return profiles from service")
    void listShouldReturnProfilesFromService() {
        SellerProfileResponse r1 = new SellerProfileResponse(
                1L, "Seller1", "1111111111", "REGON1", "KRS1",
                "Bank1", "ACC1", null, "PLN", 14, null
        );
        SellerProfileResponse r2 = new SellerProfileResponse(
                2L, "Seller2", "2222222222", "REGON2", "KRS2",
                "Bank2", "ACC2", null, "EUR", 30, null
        );

        when(sellerProfileService.getMyProfiles())
                .thenReturn(List.of(r1, r2));

        List<SellerProfileResponse> result = controller.list();

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0)).isSameAs(r1),
                () -> assertThat(result.get(1)).isSameAs(r2)
        );
        verify(sellerProfileService).getMyProfiles();
    }

    @Test
    @DisplayName("create should delegate to service and return 201 with body")
    void createShouldDelegateToServiceAndReturnCreated() {
        SellerProfileCreateRequest req = new SellerProfileCreateRequest(
                "New Seller",
                "1234567890",
                "987654321",
                "0000123456",
                "Bank",
                "PL00123456789000000000000000",
                null,
                "PLN",
                14
        );

        SellerProfileResponse created = new SellerProfileResponse(
                10L,
                "New Seller",
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

        when(sellerProfileService.createMyProfile(any(SellerProfileCreateRequest.class)))
                .thenReturn(created);

        ResponseEntity<SellerProfileResponse> response = controller.create(req);

        ArgumentCaptor<SellerProfileCreateRequest> captor =
                ArgumentCaptor.forClass(SellerProfileCreateRequest.class);
        verify(sellerProfileService).createMyProfile(captor.capture());

        SellerProfileCreateRequest passedReq = captor.getValue();

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isSameAs(created),
                () -> assertThat(passedReq.name()).isEqualTo("New Seller"),
                () -> assertThat(passedReq.nip()).isEqualTo("1234567890"),
                () -> assertThat(passedReq.defaultCurrency()).isEqualTo("PLN"),
                () -> assertThat(passedReq.defaultPaymentTermDays()).isEqualTo(14)
        );
    }

    @Test
    @DisplayName("update should delegate to service and return updated response")
    void updateShouldDelegateToServiceAndReturnUpdated() {
        Long id = 5L;

        SellerProfileUpdateRequest req = new SellerProfileUpdateRequest(
                "Updated Name",
                "9999999999",
                "REGON-NEW",
                "KRS-NEW",
                "New Bank",
                "PL00999999999999999999999999",
                null,
                "EUR",
                30,
                "INV-{YYYY}/{NNN}"
        );

        SellerProfileResponse updated = new SellerProfileResponse(
                id,
                "Updated Name",
                "9999999999",
                "REGON-NEW",
                "KRS-NEW",
                "New Bank",
                "PL00999999999999999999999999",
                null,
                "EUR",
                30,
                null
        );

        when(sellerProfileService.updateMyProfile(eq(id), any(SellerProfileUpdateRequest.class)))
                .thenReturn(updated);

        SellerProfileResponse result = controller.update(id, req);

        ArgumentCaptor<SellerProfileUpdateRequest> captor =
                ArgumentCaptor.forClass(SellerProfileUpdateRequest.class);

        verify(sellerProfileService).updateMyProfile(eq(id), captor.capture());
        SellerProfileUpdateRequest passedReq = captor.getValue();

        assertAll(
                () -> assertThat(result).isSameAs(updated),
                () -> assertThat(passedReq.name()).isEqualTo("Updated Name"),
                () -> assertThat(passedReq.nip()).isEqualTo("9999999999"),
                () -> assertThat(passedReq.defaultCurrency()).isEqualTo("EUR"),
                () -> assertThat(passedReq.defaultPaymentTermDays()).isEqualTo(30)
        );
    }

    @Test
    @DisplayName("delete should delegate to service and return 204 NO_CONTENT")
    void deleteShouldDelegateToServiceAndReturnNoContent() {
        Long id = 7L;

        ResponseEntity<Void> response = controller.delete(id);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT),
                () -> assertThat(response.getBody()).isNull()
        );
        verify(sellerProfileService).deleteMyProfile(id);
    }
}
