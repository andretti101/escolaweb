package com.andretti101.escolaweb.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da geração automática da grade horária")
public record TimetableGenerationResponseDTO(

        @Schema(description = "Total de períodos de aula alocados na grade", example = "75")
        int totalSlotsGenerated,

        @Schema(description = "Quantidade de turmas contempladas na geração", example = "3")
        int classroomsCount,

        @Schema(description = "Mensagem descritiva do resultado da geração")
        String message

) {}
