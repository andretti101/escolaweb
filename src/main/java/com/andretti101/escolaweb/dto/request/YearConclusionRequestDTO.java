package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record YearConclusionRequestDTO(

        @NotNull(message = "O ano letivo seguinte é obrigatório.")
        Integer nextYear,

        @NotNull(message = "A média mínima é obrigatória.")
        @DecimalMin(value = "0.0", message = "A média mínima deve ser no mínimo 0.")
        @DecimalMax(value = "10.0", message = "A média mínima deve ser no máximo 10.")
        BigDecimal minimumGrade,

        @NotNull(message = "A frequência mínima é obrigatória.")
        @DecimalMin(value = "0.0", message = "A frequência mínima deve ser no mínimo 0.")
        @DecimalMax(value = "100.0", message = "A frequência mínima deve ser no máximo 100.")
        BigDecimal minimumAttendance,

        @NotNull(message = "O tipo de divisão do período é obrigatório.")
        AcademicPeriodType periodType

) {}
