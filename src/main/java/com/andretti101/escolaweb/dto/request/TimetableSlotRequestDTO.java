package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record TimetableSlotRequestDTO(
        @NotNull(message = "A turma é obrigatória.")
        Integer classRoomId,

        @NotNull(message = "A vinculação professor-disciplina é obrigatória.")
        Integer teacherClassSubjectId,

        @NotNull(message = "O dia da semana é obrigatório.")
        DayOfWeekEnum dayOfWeek,

        @NotNull(message = "O horário de início é obrigatório.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @NotNull(message = "O horário de término é obrigatório.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,

        @NotNull(message = "A ordem da aula é obrigatória.")
        @Min(value = 1, message = "A ordem da aula deve ser no mínimo 1.")
        Integer slotOrder
) {}
