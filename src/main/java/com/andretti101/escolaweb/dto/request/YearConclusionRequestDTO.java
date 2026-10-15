package com.andretti101.escolaweb.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YearConclusionRequestDTO(

        @NotNull(message = "Informe o ano letivo de destino para a transição dos alunos.")
        Integer nextYear,

        @NotNull(message = "A média mínima para aprovação é obrigatória.")
        @DecimalMin(value = "0.0", message = "A média mínima para aprovação deve ser no mínimo 0,0.")
        @DecimalMax(value = "10.0", message = "A média mínima para aprovação deve ser no máximo 10,0.")
        BigDecimal minimumGrade,

        @NotNull(message = "A frequência mínima exigida é obrigatória.")
        @DecimalMin(value = "0.0", message = "A frequência mínima exigida deve ser no mínimo 0%.")
        @DecimalMax(value = "100.0", message = "A frequência mínima exigida deve ser no máximo 100%.")
        BigDecimal minimumAttendance

) {}
