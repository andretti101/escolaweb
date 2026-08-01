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

        @NotNull(message = "O vínculo professor-turma-disciplina é obrigatório.")
        Integer teacherClassSubjectId,

        @Min(value = 1, message = "A aula deve ter no mínimo 1 período.")
        @Max(value = 6, message = "A aula pode ter no máximo 6 períodos.")
        Integer lessonCount

) {}
