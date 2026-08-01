package com.andretti101.escolaweb.dto.response;

public record TokenResponseDTO(
        String token,
        String type,
        String email,
        String role,
        long expiresIn
) {}
