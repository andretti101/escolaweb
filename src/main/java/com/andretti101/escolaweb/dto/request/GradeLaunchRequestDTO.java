package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeLaunchRequestDTO(

        @NotNull(message = "O aluno é obrigatório.")
        Integer studentId,

        @NotNull(message = "A avaliação é obrigatória.")
        Integer assessmentId,

        @NotNull(message = "O valor da nota é obrigatório.")
        @DecimalMin(value = "0.0", message = "A nota deve ser no mínimo 0.")
        @DecimalMax(value = "10.0", message = "A nota deve ser no máximo 10.")
        BigDecimal value

) {}
