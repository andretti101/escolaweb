package com.andretti101.escolaweb.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcademicYearResponseDTO(
        Integer id,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdAt
) {}
