package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;

import java.util.List;

public record DivisionResponseDTO(
        AcademicPeriodType periodType,
        String label,
        int periodsCount,
        boolean hasActiveClasses,
        List<AcademicPeriodResponseDTO> periods
) {}
