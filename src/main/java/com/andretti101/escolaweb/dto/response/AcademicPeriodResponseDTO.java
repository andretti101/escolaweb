package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcademicPeriodResponseDTO(
        Integer id,
        String name,
        AcademicPeriodType periodType,
        String periodTypeLabel,
        LocalDate startDate,
        LocalDate endDate,
        boolean closed,
        Integer academicYearId,
        Integer academicYear,
        LocalDateTime createdAt
) {
    public AcademicPeriodResponseDTO(
            Integer id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            boolean closed,
            Integer academicYearId,
            Integer academicYear,
            LocalDateTime createdAt
    ) {
        this(id, name, null, null, startDate, endDate, closed, academicYearId, academicYear, createdAt);
    }
}
