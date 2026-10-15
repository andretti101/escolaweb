package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AcademicPeriodRequestDTO(

        @NotNull(message = "O ano letivo é obrigatório.")
        Integer academicYearId,

        @NotNull(message = "O tipo de divisão é obrigatório.")
        AcademicPeriodType periodType,

        @NotBlank(message = "O nome do período é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String name,

        LocalDate startDate,

        LocalDate endDate

) {}
