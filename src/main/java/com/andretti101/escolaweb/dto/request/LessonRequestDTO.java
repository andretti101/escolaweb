package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LessonRequestDTO(

        @NotNull(message = "A data da aula é obrigatória.")
        LocalDate lessonDate,

        String content,

        String notes,

        @NotNull(message = "Selecione a disciplina e a turma correspondentes à aula.")
        Integer teacherClassSubjectId,

        @Min(value = 1, message = "A quantidade de períodos da aula deve ser no mínimo 1.")
        @Max(value = 6, message = "A quantidade de períodos da aula pode ser no máximo 6.")
        Integer lessonCount

) {}
