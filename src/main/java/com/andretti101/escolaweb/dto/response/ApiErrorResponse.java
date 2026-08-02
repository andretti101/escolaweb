package com.andretti101.escolaweb.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> errors
) {

    public record FieldErrorDetail(String field, String message) {}

    // ── Factory methods

    public static ApiErrorResponse of(HttpStatus httpStatus, String message, String path) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                path,
                List.of()
        );
    }

    public static ApiErrorResponse ofValidation(
            HttpStatus httpStatus,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors) {
        return new ApiErrorResponse(
                LocalDateTime.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
    }
}
