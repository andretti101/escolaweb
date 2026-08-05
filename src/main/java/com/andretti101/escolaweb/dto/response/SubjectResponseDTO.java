package com.andretti101.escolaweb.dto.response;

import java.time.LocalDateTime;

public record SubjectResponseDTO(
        Integer id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt
) {}
