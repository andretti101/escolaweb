package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EnrollmentRequestDTO(

        @NotNull(message = "Selecione o aluno que deseja matricular.")
        Integer studentId,

        @NotNull(message = "Selecione a turma para a matrícula.")
        Integer classRoomId,

        LocalDate enrollmentDate

) {}
