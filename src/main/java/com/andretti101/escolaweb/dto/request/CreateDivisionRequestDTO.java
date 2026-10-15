package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import jakarta.validation.constraints.NotNull;

public record CreateDivisionRequestDTO(
        @NotNull(message = "O ano letivo é obrigatório.")
        Integer academicYearId,

        @NotNull(message = "O tipo de divisão é obrigatório.")
        AcademicPeriodType periodType
) {}
