package com.andretti101.escolaweb.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SchoolSettingsResponseDTO(
        Integer id,
        String schoolName,
        String cep,
        String address,
        String phone,
        String email,
        BigDecimal minimumGrade,
        BigDecimal minimumAttendance,
        LocalDateTime createdAt
) {
    public SchoolSettingsResponseDTO(
            Integer id,
            String schoolName,
            String address,
            String phone,
            String email,
            BigDecimal minimumGrade,
            BigDecimal minimumAttendance,
            LocalDateTime createdAt
    ) {
        this(id, schoolName, null, address, phone, email, minimumGrade, minimumAttendance, createdAt);
    }
}
