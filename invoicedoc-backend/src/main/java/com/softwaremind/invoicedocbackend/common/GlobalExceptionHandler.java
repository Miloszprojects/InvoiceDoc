package com.softwaremind.invoicedocbackend.common;

import com.softwaremind.invoicedocbackend.common.dto.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex) {
        String code = ex.getReason();

        ApiError body = new ApiError(
                code,
                code
        );

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        ApiError body = new ApiError(
                "UNEXPECTED_ERROR",
                "Unexpected error"
        );
        return ResponseEntity.status(500).body(body);
    }
}
