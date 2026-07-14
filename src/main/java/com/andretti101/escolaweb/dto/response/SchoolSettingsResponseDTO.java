package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SchoolSettingsResponseDTO(
        Integer id,
        String schoolName,
        String address,
        String phone,
        String email,
        BigDecimal minimumGrade,
        BigDecimal minimumAttendance,
        AcademicPeriodType periodType,
        LocalDateTime createdAt
) {}
