package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(

        @NotBlank(message = "O refresh token é obrigatório.")
        String refreshToken

) {}
