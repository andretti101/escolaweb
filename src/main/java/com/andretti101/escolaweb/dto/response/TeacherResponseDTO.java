package com.andretti101.escolaweb.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TeacherResponseDTO(
        Integer id,
        String name,
        String email,
        boolean active,
        LocalDateTime createdAt,
        List<SubjectResponseDTO> subjects
) {
    public TeacherResponseDTO(
            Integer id,
            String name,
            String email,
            boolean active,
            LocalDateTime createdAt
    ) {
        this(id, name, email, active, createdAt, List.of());
    }
}
