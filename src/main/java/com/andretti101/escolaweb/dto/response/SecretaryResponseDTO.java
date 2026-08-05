package com.andretti101.escolaweb.dto.response;

import java.time.LocalDateTime;

public record SecretaryResponseDTO(
        Integer id,
        String name,
        String email,
        boolean active,
        LocalDateTime createdAt
) {}
