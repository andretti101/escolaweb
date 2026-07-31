package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TeacherClassSubjectRequestDTO(

        @NotNull(message = "O professor é obrigatório.")
        Integer teacherId,

        @NotNull(message = "A turma é obrigatória.")
        Integer classRoomId,

        @NotNull(message = "A disciplina é obrigatória.")
        Integer subjectId,

        @Min(value = 1, message = "O número mínimo de avaliações deve ser ao menos 1.")
        Integer minAssessmentsPerPeriod,

        @Min(value = 1, message = "O número máximo de avaliações deve ser ao menos 1.")
        Integer maxAssessmentsPerPeriod

) {}
