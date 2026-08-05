package com.andretti101.escolaweb.dto.response;

import java.time.LocalDateTime;

public record PrincipalResponseDTO(
        Integer id,
        String name,
        String email,
        boolean active,
        LocalDateTime createdAt
) {}
