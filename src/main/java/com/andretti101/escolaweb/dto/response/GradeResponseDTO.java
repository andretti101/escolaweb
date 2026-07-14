package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.StudentSituation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GradeResponseDTO(
        Integer id,
        Integer studentId,
        String studentName,
        Integer assessmentId,
        String assessmentTitle,
        BigDecimal value,
        StudentSituation studentSituation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
