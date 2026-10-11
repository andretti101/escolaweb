package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GradeLaunchRequestDTO(

        @NotNull(message = "Selecione o aluno para lançar a nota.")
        Integer studentId,

        @NotNull(message = "Selecione a avaliação correspondente.")
        Integer assessmentId,

        @NotNull(message = "O valor da nota é obrigatório.")
        @DecimalMin(value = "0.0", message = "A nota deve ser igual ou superior a 0,0.")
        @DecimalMax(value = "10.0", message = "A nota não pode ser superior a 10,0.")
        BigDecimal value

) {}
