package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AnnouncementRequestDTO(

        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String title,

        @NotBlank(message = "A mensagem é obrigatória.")
        @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres.")
        String message,

        LocalDate eventDate,

        List<Integer> classRoomIds,

        Boolean targetTeachers

) {}
