package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.response.ApiErrorResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping({"/error", "/erro"})
    public Object handleError(
            @RequestParam(value = "status", required = false) Integer statusParam,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = 500;
        if (statusParam != null) {
            statusCode = statusParam;
        } else if (statusAttr != null) {
            try {
                statusCode = Integer.parseInt(statusAttr.toString());
            } catch (NumberFormatException ignored) {
                statusCode = 500;
            }
        }

        HttpStatus httpStatus = HttpStatus.resolve(statusCode);
        if (httpStatus == null) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            statusCode = 500;
        }

        String originalUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (originalUri == null || originalUri.isBlank()) {
            originalUri = request.getRequestURI();
        }

        String title;
        String description;
        if (statusCode == 403) {
            title = "Acesso Restrito";
            description = "Você não possui permissão para acessar esta página ou recurso.";
        } else if (statusCode == 404) {
            title = "Página Não Encontrada";
            description = "O endereço solicitado não existe ou foi movido.";
        } else if (statusCode == 401) {
            title = "Sessão Não Autenticada";
            description = "Sua sessão expirou ou você precisa entrar no sistema para acessar este recurso.";
        } else {
            title = "Erro Interno do Sistema";
            description = "Ocorreu uma falha inesperada ao processar sua solicitação. Tente novamente em instantes.";
        }

        String accept = request.getHeader("Accept");
        boolean isApiRequest = (originalUri != null && originalUri.startsWith("/api/"))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE) && !accept.contains(MediaType.TEXT_HTML_VALUE));

        if (isApiRequest) {
            return ResponseEntity
                    .status(httpStatus)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiErrorResponse.of(httpStatus, description, originalUri));
        }

        response.setStatus(statusCode);
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorDescription", description);
        model.addAttribute("requestUri", originalUri);
        return "error";
    }
}
