package com.andretti101.escolaweb.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcademicYearResponseDTO(
        Integer id,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        LocalDateTime createdAt,
        BigDecimal minimumGrade,
        BigDecimal minimumAttendance
) {
    public AcademicYearResponseDTO(
            Integer id,
            Integer year,
            LocalDate startDate,
            LocalDate endDate,
            boolean active,
            LocalDateTime createdAt
    ) {
        this(id, year, startDate, endDate, active, createdAt,
                new BigDecimal("6.00"), new BigDecimal("75.00"));
    }
}
