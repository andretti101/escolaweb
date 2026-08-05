package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(

        @NotBlank(message = "A senha atual é obrigatória.")
        String currentPassword,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 255, message = "A nova senha deve ter entre 8 e 255 caracteres.")
        String newPassword

) {}
