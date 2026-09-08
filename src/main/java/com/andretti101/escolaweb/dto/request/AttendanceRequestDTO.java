package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceRequestDTO(

        @NotNull(message = "O aluno é obrigatório.")
        Integer studentId,

        @NotNull(message = "A aula é obrigatória.")
        Integer lessonId,

        @NotBlank(message = "O status da presença é obrigatório.")
        String status

) {}
