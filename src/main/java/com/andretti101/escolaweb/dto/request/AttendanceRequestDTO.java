package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceRequestDTO(

        @NotNull(message = "O aluno é obrigatório.")
        Integer studentId,

        @NotNull(message = "A aula é obrigatória.")
        Integer lessonId,

        @NotNull(message = "O status da presença é obrigatório.")
        AttendanceStatus status,

        String notes

) {}
