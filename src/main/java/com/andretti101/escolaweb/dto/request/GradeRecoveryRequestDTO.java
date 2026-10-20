package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GradeRecoveryRequestDTO(

        @NotNull(message = "O valor da nota de recuperação é obrigatório.")
        @DecimalMin(value = "0.0", message = "A nota de recuperação deve ser igual ou superior a 0,0.")
        @DecimalMax(value = "10.0", message = "A nota de recuperação não pode ser superior a 10,0.")
        BigDecimal recoveryValue,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String notes

) {
    public GradeRecoveryRequestDTO {
        if (notes != null) {
            notes = notes.trim();
            if (notes.isEmpty()) {
                notes = null;
            }
        }
    }

    public GradeRecoveryRequestDTO(BigDecimal recoveryValue) {
        this(recoveryValue, null);
    }
}
