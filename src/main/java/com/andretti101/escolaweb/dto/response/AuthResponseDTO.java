package com.andretti101.escolaweb.dto.response;

// DTO imutável retornado após login bem-sucedido com tokens JWT, expiração e dados do perfil RBAC
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
