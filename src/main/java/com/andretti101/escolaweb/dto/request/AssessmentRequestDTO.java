package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.AssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssessmentRequestDTO(

        @NotBlank(message = "O título da avaliação é obrigatório.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String title,

        String description,

        @NotNull(message = "A data da avaliação é obrigatória.")
        LocalDate date,

        @NotNull(message = "O tipo de avaliação é obrigatório.")
        AssessmentType assessmentType,

        @NotNull(message = "O vínculo professor-turma-disciplina é obrigatório.")
        Integer teacherClassSubjectId,

        @NotNull(message = "O período letivo é obrigatório.")
        Integer periodId

) {}
