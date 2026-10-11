package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TeacherClassSubjectRequestDTO(

        @NotNull(message = "Selecione o professor responsável.")
        Integer teacherId,

        @NotNull(message = "Selecione a turma correspondente.")
        Integer classRoomId,

        @NotNull(message = "Selecione a disciplina correspondente.")
        Integer subjectId,

        @Min(value = 1, message = "O número mínimo de avaliações por período deve ser no mínimo 1.")
        Integer minAssessmentsPerPeriod,

        @Min(value = 1, message = "O número máximo de avaliações por período deve ser no mínimo 1.")
        Integer maxAssessmentsPerPeriod

) {}
