package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequestDTO(

        @NotBlank(message = "O nome do aluno é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String name,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 255, message = "A senha deve ter entre 8 e 255 caracteres.")
        String password,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String ownPhone,

        @Size(max = 20, message = "O telefone do responsável deve ter no máximo 20 caracteres.")
        String guardianPhone,

        @NotBlank(message = "O número de matrícula é obrigatório.")
        @Size(max = 30, message = "A matrícula deve ter no máximo 30 caracteres.")
        String registrationNumber,

        @NotNull(message = "A data de nascimento é obrigatória.")
        @Past(message = "A data de nascimento deve ser uma data passada.")
        LocalDate birthDate

) {}
