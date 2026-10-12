package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GradeUpdateRequestDTO(

        @NotNull(message = "O valor da nota é obrigatório.")
        @DecimalMin(value = "0.0", message = "A nota deve ser igual ou superior a 0,0.")
        @DecimalMax(value = "10.0", message = "A nota não pode ser superior a 10,0.")
        BigDecimal value,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String notes

) {
    public GradeUpdateRequestDTO {
        if (notes != null) {
            notes = notes.trim();
            if (notes.isEmpty()) {
                notes = null;
            }
        }
    }

    public GradeUpdateRequestDTO(BigDecimal value) {
        this(value, null);
    }
}

