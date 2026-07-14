package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EnrollmentRequestDTO(

        @NotNull(message = "O aluno é obrigatório.")
        Integer studentId,

        @NotNull(message = "A turma é obrigatória.")
        Integer classRoomId,

        LocalDate enrollmentDate

) {}
