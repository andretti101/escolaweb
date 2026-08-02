package com.andretti101.escolaweb.dto.response;

public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        String email,
        String role,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {}
