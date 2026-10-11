package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(

        @NotBlank(message = "A sessão expirou. Faça login novamente para continuar.")
        String refreshToken

) {}
