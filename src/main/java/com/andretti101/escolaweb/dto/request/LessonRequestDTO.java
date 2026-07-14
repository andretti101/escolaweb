package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LessonRequestDTO(

        @NotNull(message = "A data da aula é obrigatória.")
        LocalDate lessonDate,

        String content,

        String notes,

        @NotNull(message = "O vínculo professor-turma-disciplina é obrigatório.")
        Integer teacherClassSubjectId

) {}
