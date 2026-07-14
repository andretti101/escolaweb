package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SchoolSettingsRequestDTO(

        @Size(max = 150, message = "O nome da escola deve ter no máximo 150 caracteres.")
        String schoolName,

        @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres.")
        String address,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String phone,

        @Email(message = "O e-mail informado é inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @DecimalMin(value = "0.0", message = "A média mínima deve ser no mínimo 0.")
        @DecimalMax(value = "10.0", message = "A média mínima deve ser no máximo 10.")
        BigDecimal minimumGrade,

        @DecimalMin(value = "0.0", message = "A frequência mínima deve ser no mínimo 0.")
        @DecimalMax(value = "100.0", message = "A frequência mínima deve ser no máximo 100.")
        BigDecimal minimumAttendance,

        AcademicPeriodType periodType

) {}
