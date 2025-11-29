package com.softwaremind.invoicedocbackend.contractor;

import com.softwaremind.invoicedocbackend.contractor.dto.ContractorCreateRequest;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorResponse;
import com.softwaremind.invoicedocbackend.contractor.dto.ContractorUpdateRequest;
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
class ContractorControllerTest {

    @Mock
    private ContractorService contractorService;

    @InjectMocks
    private ContractorController controller;

    @Test
    @DisplayName("list should delegate to service with null query and return list")
    void listShouldDelegateToServiceWithNullQuery() {
        ContractorResponse r1 = new ContractorResponse(
                1L, ContractorType.COMPANY, "ACME", "123", null, null,
                "acme@example.com", "+48 123 456 789", true
        );
        ContractorResponse r2 = new ContractorResponse(
                2L, ContractorType.PERSON, "John Doe", null, "90010112345", null,
                "john.doe@example.com", "+48 987 654 321", false
        );

        when(contractorService.list(null)).thenReturn(List.of(r1, r2));

        List<ContractorResponse> result = controller.list(null);

        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0)).isSameAs(r1),
                () -> assertThat(result.get(1)).isSameAs(r2)
        );
        verify(contractorService).list(null);
    }

    @Test
    @DisplayName("list should delegate to service with query param when present")
    void listShouldDelegateToServiceWithQuery() {
        String query = "acme";
        ContractorResponse r = new ContractorResponse(
                1L, ContractorType.COMPANY, "ACME", "123", null, null,
                "acme@example.com", "+48 123 456 789", true
        );

        when(contractorService.list(query)).thenReturn(List.of(r));

        List<ContractorResponse> result = controller.list(query);

        assertAll(
                () -> assertThat(result).hasSize(1),
                () -> assertThat(result.get(0)).isSameAs(r)
        );
        verify(contractorService).list(query);
    }

    @Test
    @DisplayName("get should delegate to service and return contractor response")
    void getShouldDelegateToServiceAndReturnResponse() {
        Long id = 10L;
        ContractorResponse response = new ContractorResponse(
                id, ContractorType.COMPANY, "ACME", "1234567890", null, null,
                "acme@example.com", "+48 123 456 789", true
        );

        when(contractorService.get(id)).thenReturn(response);

        ContractorResponse result = controller.get(id);

        assertThat(result).isSameAs(response);
        verify(contractorService).get(id);
    }

    @Test
    @DisplayName("create should delegate to service and return 201 CREATED with body")
    void createShouldDelegateToServiceAndReturnCreated() {
        ContractorCreateRequest req = new ContractorCreateRequest(
                ContractorType.COMPANY,
                "New Contractor",
                "1234567890",
                null,
                null,
                "new@example.com",
                "+48 111 222 333",
                true
        );

        ContractorResponse created = new ContractorResponse(
                100L,
                ContractorType.COMPANY,
                "New Contractor",
                "1234567890",
                null,
                null,
                "new@example.com",
                "+48 111 222 333",
                true
        );

        when(contractorService.create(any(ContractorCreateRequest.class)))
                .thenReturn(created);

        ResponseEntity<ContractorResponse> response = controller.create(req);

        ArgumentCaptor<ContractorCreateRequest> captor =
                ArgumentCaptor.forClass(ContractorCreateRequest.class);
        verify(contractorService).create(captor.capture());

        ContractorCreateRequest passedReq = captor.getValue();

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isSameAs(created),
                () -> assertThat(passedReq.type()).isEqualTo(ContractorType.COMPANY),
                () -> assertThat(passedReq.name()).isEqualTo("New Contractor"),
                () -> assertThat(passedReq.nip()).isEqualTo("1234567890"),
                () -> assertThat(passedReq.email()).isEqualTo("new@example.com"),
                () -> assertThat(passedReq.phone()).isEqualTo("+48 111 222 333"),
                () -> assertThat(passedReq.favorite()).isTrue()
        );
    }

    @Test
    @DisplayName("update should delegate to service and return updated response")
    void updateShouldDelegateToServiceAndReturnUpdatedResponse() {
        Long id = 20L;

        ContractorUpdateRequest req = new ContractorUpdateRequest(
                ContractorType.PERSON,
                "Updated Name",
                null,
                "90010112345",
                null,
                "updated@example.com",
                "+48 999 888 777",
                false
        );

        ContractorResponse updated = new ContractorResponse(
                id,
                ContractorType.PERSON,
                "Updated Name",
                null,
                "90010112345",
                null,
                "updated@example.com",
                "+48 999 888 777",
                false
        );

        when(contractorService.update(eq(id), any(ContractorUpdateRequest.class)))
                .thenReturn(updated);

        ContractorResponse result = controller.update(id, req);

        ArgumentCaptor<ContractorUpdateRequest> captor =
                ArgumentCaptor.forClass(ContractorUpdateRequest.class);
        verify(contractorService).update(eq(id), captor.capture());

        ContractorUpdateRequest passedReq = captor.getValue();

        assertAll(
                () -> assertThat(result).isSameAs(updated),
                () -> assertThat(passedReq.type()).isEqualTo(ContractorType.PERSON),
                () -> assertThat(passedReq.name()).isEqualTo("Updated Name"),
                () -> assertThat(passedReq.pesel()).isEqualTo("90010112345"),
                () -> assertThat(passedReq.email()).isEqualTo("updated@example.com"),
                () -> assertThat(passedReq.phone()).isEqualTo("+48 999 888 777"),
                () -> assertThat(passedReq.favorite()).isFalse()
        );
    }

    @Test
    @DisplayName("delete should delegate to service and return 204 NO_CONTENT")
    void deleteShouldDelegateToServiceAndReturnNoContent() {
        Long id = 30L;

        ResponseEntity<Void> response = controller.delete(id);

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT),
                () -> assertThat(response.getBody()).isNull()
        );
        verify(contractorService).delete(id);
    }
}
