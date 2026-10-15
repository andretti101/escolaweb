package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentPatchRequestDTO(
        @Pattern(regexp = ".*\\S.*", message = "O nome não pode ser vazio.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String name,

        @Pattern(regexp = ".*\\S.*", message = "O e-mail informado é inválido.")
        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @Size(min = 8, max = 255, message = "A senha deve ter entre 8 e 255 caracteres.")
        String password,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String ownPhone,

        @Size(max = 20, message = "O telefone do responsável deve ter no máximo 20 caracteres.")
        String guardianPhone,

        @Pattern(regexp = ".*\\S.*", message = "A matrícula não pode ser vazia.")
        @Size(max = 30, message = "A matrícula deve ter no máximo 30 caracteres.")
        String registrationNumber,

        @Past(message = "A data de nascimento deve ser anterior à data de hoje.")
        LocalDate birthDate
) {
    public StudentPatchRequestDTO {
        if (password != null && password.isBlank()) {
            password = null;
        }
    }
}
