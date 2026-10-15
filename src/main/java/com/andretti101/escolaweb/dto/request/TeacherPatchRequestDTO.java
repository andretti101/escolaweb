package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TeacherPatchRequestDTO(
        @Pattern(regexp = ".*\\S.*", message = "O nome não pode ser vazio.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String name,

        @Pattern(regexp = ".*\\S.*", message = "O e-mail informado é inválido.")
        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @Size(min = 8, max = 255, message = "A senha deve ter entre 8 e 255 caracteres.")
        String password,

        List<Integer> subjectIds
) {
    public TeacherPatchRequestDTO {
        if (password != null && password.isBlank()) {
            password = null;
        }
    }
}
