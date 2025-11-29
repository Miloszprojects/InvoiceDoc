package com.softwaremind.invoicedocbackend.common;

import com.softwaremind.invoicedocbackend.common.dto.ApiError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleResponseStatusException should return same HTTP status and ApiError with reason as code/message")
    void handleResponseStatusExceptionShouldReturnStatusAndApiError() {
        String reason = "SELLER_PROFILE_NOT_FOUND";
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.NOT_FOUND, reason);

        ResponseEntity<ApiError> response = handler.handleResponseStatusException(ex);

        ApiError body = response.getBody();

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(body).isNotNull(),
                () -> assertThat(body.code()).isEqualTo(reason),
                () -> assertThat(body.message()).isEqualTo(reason)
        );
    }

    @Test
    @DisplayName("handleResponseStatusException should handle null reason (code and message null)")
    void handleResponseStatusExceptionShouldHandleNullReason() {
        ResponseStatusException ex =
                new ResponseStatusException(HttpStatus.BAD_REQUEST, null);

        ResponseEntity<ApiError> response = handler.handleResponseStatusException(ex);

        ApiError body = response.getBody();

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(body).isNotNull(),
                () -> assertThat(body.code()).isNull(),
                () -> assertThat(body.message()).isNull()
        );
    }

    @Test
    @DisplayName("handleOther should return 500 and generic UNEXPECTED_ERROR ApiError")
    void handleOtherShouldReturn500AndUnexpectedError() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ApiError> response = handler.handleOther(ex);

        ApiError body = response.getBody();

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(body).isNotNull(),
                () -> assertThat(body.code()).isEqualTo("UNEXPECTED_ERROR"),
                () -> assertThat(body.message()).isEqualTo("Unexpected error")
        );
    }
}
