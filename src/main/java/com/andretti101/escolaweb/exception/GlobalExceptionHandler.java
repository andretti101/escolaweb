package com.andretti101.escolaweb.exception;

import com.andretti101.escolaweb.dto.response.ApiErrorResponse;
import com.andretti101.escolaweb.dto.response.ApiErrorResponse.FieldErrorDetail;
import com.andretti101.escolaweb.service.exception.UnauthorizedOperationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

// Interceptador global de exceções da API REST (RFC 7807 Problem Details)
// Converte erros técnicos e violações de regras em mensagens amigáveis internacionalizadas (i18n)
@Slf4j
@RestControllerAdvice
@lombok.RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final org.springframework.context.MessageSource messageSource;

    // Obtém a mensagem traduzida do messages_pt_BR.properties de acordo com o Locale atual
    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
    }

    // ═════════════════════════════
    // 400 BAD REQUEST - Validações de entrada (@Valid / DTOs)
    // ═════════════════════════════

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .orElse(getMessage("error.validation"));

        log.warn("Validation failed on {}: {}", request.getRequestURI(), errorMessage);

        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> new FieldErrorDetail(f.getField(), f.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.ofValidation(
                        HttpStatus.BAD_REQUEST,
                        errorMessage,
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        String errorMessage = ex.getConstraintViolations()
                .stream()
                .findFirst()
                .map(jakarta.validation.ConstraintViolation::getMessage)
                .orElse(getMessage("error.invalid.param"));

        log.warn("Constraint violation on {}: {}", request.getRequestURI(), errorMessage);

        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> new FieldErrorDetail(
                        cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "param",
                        cv.getMessage()))
                .toList();

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.ofValidation(
                        HttpStatus.BAD_REQUEST,
                        errorMessage,
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        getMessage("error.malformed.body"),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing request parameter on {}: {}", request.getRequestURI(), ex.getParameterName());

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        getMessage("error.missing.param", ex.getParameterName()),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String expected = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "desconhecido";

        log.warn("Type mismatch on {}: param '{}' received '{}', expected {}",
                request.getRequestURI(), ex.getName(), ex.getValue(), expected);

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        getMessage("error.type.mismatch", ex.getName()),
                        request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : getMessage("error.illegal.argument");

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        message,
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 401 UNAUTHORIZED
    // ═════════════════════════════

    @ExceptionHandler({BadCredentialsException.class, org.springframework.security.core.AuthenticationException.class})
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            Exception ex, HttpServletRequest request) {

        log.warn("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        getMessage("error.bad.credentials"),
                        request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {

        log.warn("Login attempt by disabled account on {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        getMessage("error.disabled.account"),
                        request.getRequestURI()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex, HttpServletRequest request) {

        log.warn("Username not found on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        getMessage("error.bad.credentials"),
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 403 FORBIDDEN
    // ═════════════════════════════

    @ExceptionHandler({AccessDeniedException.class, SecurityException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            Exception ex, HttpServletRequest request) {

        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : getMessage("error.access.denied");

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN,
                        message,
                        request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedOperation(
            UnauthorizedOperationException ex, HttpServletRequest request) {

        log.warn("Unauthorized operation on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : getMessage("error.unauthorized.operation");

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN,
                        message,
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 404 NOT FOUND
    // ═════════════════════════════

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {

        log.warn("Entity not found on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : getMessage("error.entity.not.found");

        if (message != null && (message.contains("com.andretti101") || message.toLowerCase().startsWith("unable to find") || message.toLowerCase().contains("entity"))) {
            message = getMessage("error.entity.not.found");
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND,
                        message,
                        request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("No route found: {} {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND,
                        getMessage("error.route.not.found", request.getMethod(), request.getRequestURI()),
                        request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            org.springframework.web.server.ResponseStatusException ex, HttpServletRequest request) {

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.of(
                        status,
                        message,
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 405 METHOD NOT ALLOWED
    // ═════════════════════════════

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Method not allowed: {} {}", request.getMethod(), request.getRequestURI());

        String supported = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods().toString()
                : "N/A";

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiErrorResponse.of(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        getMessage("error.method.not.allowed", ex.getMethod(), supported),
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 415 UNSUPPORTED MEDIA TYPE
    // ═════════════════════════════

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        log.warn("Unsupported media type on {}: {}", request.getRequestURI(), ex.getContentType());

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        getMessage("error.unsupported.media.type", ex.getContentType()),
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 409 CONFLICT
    // ═════════════════════════════

    @ExceptionHandler(com.andretti101.escolaweb.service.exception.ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            com.andretti101.escolaweb.service.exception.ConflictException ex, HttpServletRequest request) {

        log.warn("Conflict on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT,
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String rootCause = ex.getRootCause() != null
                ? ex.getRootCause().getMessage()
                : ex.getMessage();

        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), rootCause);

        String userMessage = getMessage("error.data.conflict");
        
        if (rootCause != null) {
            String lower = rootCause.toLowerCase();
            if (lower.contains("foreign key") || lower.contains("is still referenced") || lower.contains("referenced from table") || lower.contains("violates foreign key")) {
                userMessage = getMessage("error.foreign.key.conflict");
            } else if (lower.contains("enrollment") || lower.contains("uk3vna4k5oncxyxln26gmj708i2") || lower.contains("registration") || lower.contains("students_enrollment_key")) {
                userMessage = getMessage("error.enrollment.in.use");
            } else if (lower.contains("email") || lower.contains("uk_users_email") || lower.contains("users_email_key")) {
                userMessage = getMessage("error.email.in.use");
            } else if (lower.contains("subject") || lower.contains("uk_subjects_name") || lower.contains("subjects_name_key")) {
                userMessage = getMessage("error.subject.in.use");
            } else if (lower.contains("class_room") || lower.contains("classroom") || lower.contains("class_rooms")) {
                userMessage = getMessage("error.classroom.in.use");
            } else if (lower.contains("academic_year") || lower.contains("academicyear") || lower.contains("academic_years")) {
                userMessage = getMessage("error.academic.year.in.use");
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT,
                        userMessage,
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 422 UNPROCESSABLE ENTITY
    // ═════════════════════════════

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Business rule violation on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : getMessage("error.illegal.state");

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        message,
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 500 INTERNAL SERVER ERROR
    // ═════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error on {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        getMessage("error.internal.server"),
                        request.getRequestURI()));
    }
}


