package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String password

) {}
