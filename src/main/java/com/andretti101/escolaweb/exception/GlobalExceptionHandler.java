package com.andretti101.escolaweb.exception;

import com.andretti101.escolaweb.dto.response.ApiErrorResponse;
import com.andretti101.escolaweb.dto.response.ApiErrorResponse.FieldErrorDetail;
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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ═════════════════════════════
    // 400 BAD REQUEST
    // ═════════════════════════════

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldErrorDetail> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new FieldErrorDetail(
                        fe.getField(),
                        fe.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.ofValidation(
                        HttpStatus.BAD_REQUEST,
                        "Erro de validação nos campos enviados.",
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String field = cv.getPropertyPath().toString();
                    if (field.contains(".")) {
                        field = field.substring(field.lastIndexOf('.') + 1);
                    }
                    return new FieldErrorDetail(field, cv.getMessage());
                })
                .collect(Collectors.toList());

        log.warn("Constraint violation on {}: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.ofValidation(
                        HttpStatus.BAD_REQUEST,
                        "Parâmetro inválido na requisição.",
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
                        "Corpo da requisição inválido ou malformado. Verifique o JSON enviado.",
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
                        "Parâmetro obrigatório ausente: '" + ex.getParameterName() + "'.",
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
                        String.format(
                                "Parâmetro '%s' com valor inválido '%s'. Tipo esperado: %s.",
                                ex.getName(), ex.getValue(), expected),
                        request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(
                        HttpStatus.BAD_REQUEST,
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 401 UNAUTHORIZED
    // ═════════════════════════════

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("Bad credentials on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        ex.getMessage(),
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
                        "Conta desativada. Entre em contato com a secretaria.",
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
                        "Credenciais inválidas.",
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 403 FORBIDDEN
    // ═════════════════════════════

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado. Você não tem permissão para realizar esta operação.",
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 404 NOT FOUND
    // ═════════════════════════════

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {

        log.warn("Entity not found on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage(),
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
                        "Rota não encontrada: " + request.getMethod() + " " + request.getRequestURI(),
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
                        String.format(
                                "Método HTTP '%s' não permitido para esta rota. Métodos aceitos: %s.",
                                ex.getMethod(), supported),
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
                        "Content-Type não suportado: " + ex.getContentType()
                                + ". Use 'application/json'.",
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 409 CONFLICT
    // ═════════════════════════════

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String rootCause = ex.getRootCause() != null
                ? ex.getRootCause().getMessage()
                : ex.getMessage();

        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), rootCause);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(
                        HttpStatus.CONFLICT,
                        "Conflito de dados: registro duplicado ou violação de integridade. "
                                + "Verifique se o recurso já existe.",
                        request.getRequestURI()));
    }

    // ═════════════════════════════
    // 422 UNPROCESSABLE ENTITY
    // ═════════════════════════════

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Business rule violation on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        ex.getMessage(),
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
                        "Ocorreu um erro interno inesperado. Tente novamente mais tarde.",
                        request.getRequestURI()));
    }
}
