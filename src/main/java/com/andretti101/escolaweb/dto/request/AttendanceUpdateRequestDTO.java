package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AttendanceUpdateRequestDTO(

        @NotBlank(message = "O status da presença é obrigatório. Selecione presente ou ausente.")
        String status

) {}
