package com.andretti101.escolaweb.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcademicPeriodResponseDTO(
        Integer id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean closed,
        Integer academicYearId,
        Integer academicYear,
        LocalDateTime createdAt
) {}
