package com.andretti101.escolaweb.dto.response;

public record AuthResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        String email,
        String name,
        String role,
        Integer userId,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {}
