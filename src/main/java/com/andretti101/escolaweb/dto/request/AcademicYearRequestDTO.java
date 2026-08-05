package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AcademicYearRequestDTO(

        @NotNull(message = "O ano letivo é obrigatório.")
        @Min(value = 2000, message = "O ano deve ser a partir de 2000.")
        @Max(value = 2100, message = "O ano deve ser até 2100.")
        Integer year,

        LocalDate startDate,

        LocalDate endDate

) {}
