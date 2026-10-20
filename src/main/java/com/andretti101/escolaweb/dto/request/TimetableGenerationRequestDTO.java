package com.andretti101.escolaweb.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalTime;
import java.util.List;

@Schema(description = "Parâmetros para a geração automática da grade horária")
public record TimetableGenerationRequestDTO(

        @Schema(description = "Tempo/duração de uma aula completa em minutos (ex: 45 ou 50, opcional se horários forem calculados)", example = "45")
        @Min(value = 30, message = "A aula deve ter no mínimo 30 minutos.")
        @Max(value = 120, message = "A aula deve ter no máximo 120 minutos.")
        Integer lessonDurationMinutes,

        @Schema(description = "Horário de início das aulas (legado / matutino)", example = "07:50")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,

        @Schema(description = "Horário de entrada das aulas do turno matutino (ex: 07:50)", example = "07:50")
        @JsonFormat(pattern = "HH:mm")
        LocalTime morningStartTime,

        @Schema(description = "Horário de saída das aulas do turno matutino (ex: 11:45 para 5 aulas ou 12:30 para 6 aulas)", example = "11:45")
        @JsonFormat(pattern = "HH:mm")
        LocalTime morningEndTime,

        @Schema(description = "Horário de entrada das aulas do turno vespertino (ex: 13:15)", example = "13:15")
        @JsonFormat(pattern = "HH:mm")
        LocalTime afternoonStartTime,

        @Schema(description = "Horário de saída das aulas do turno vespertino (ex: 17:10 ou 18:00)", example = "17:10")
        @JsonFormat(pattern = "HH:mm")
        LocalTime afternoonEndTime,

        @Schema(description = "Horário de entrada das aulas do turno noturno (ex: 18:30 ou 19:00)", example = "19:00")
        @JsonFormat(pattern = "HH:mm")
        LocalTime nightStartTime,

        @Schema(description = "Horário de saída das aulas do turno noturno (ex: 22:00 ou 22:30)", example = "22:30")
        @JsonFormat(pattern = "HH:mm")
        LocalTime nightEndTime,

        @Schema(description = "Duração do intervalo/recreio em minutos (padrão: 10 min)", example = "10")
        @Min(value = 0, message = "O intervalo não pode ser negativo.")
        @Max(value = 60, message = "O intervalo não pode exceder 60 minutos.")
        Integer breakDurationMinutes,

        @Schema(description = "IDs das turmas que possuem a 6ª aula (exclusivo para turnos Matutino e Vespertino)")
        List<Integer> classroomsWithSixthLessonIds,

        @Schema(description = "ID do ano letivo (opcional, padrão: ano letivo ativo)")
        Integer academicYearId,

        @Schema(description = "IDs específicos das turmas a serem geradas (opcional, padrão: todas as turmas ativas do ano letivo)")
        List<Integer> classRoomIds

) {}
