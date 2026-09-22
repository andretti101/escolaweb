package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AttendanceUpdateRequestDTO(

        @NotBlank(message = "O status da presen\u00e7a \u00e9 obrigat\u00f3rio.")
        String status

) {}
