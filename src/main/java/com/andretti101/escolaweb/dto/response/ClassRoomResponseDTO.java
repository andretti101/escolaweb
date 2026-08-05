package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.Shift;

import java.time.LocalDateTime;

public record ClassRoomResponseDTO(
        Integer id,
        String name,
        Shift shift,
        Integer creationYear,
        Integer academicYearId,
        Integer academicYear,
        boolean active,
        LocalDateTime createdAt
) {}
