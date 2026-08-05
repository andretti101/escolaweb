package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.Shift;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClassRoomRequestDTO(

        @NotNull(message = "O ano letivo é obrigatório.")
        Integer academicYearId,

        @NotBlank(message = "O nome da turma é obrigatório.")
        @Size(max = 20, message = "O nome deve ter no máximo 20 caracteres.")
        String name,

        @NotNull(message = "O turno é obrigatório.")
        Shift shift,

        Integer creationYear

) {}
