package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AssessmentType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssessmentResponseDTO(
        Integer id,
        String title,
        String description,
        LocalDate date,
        AssessmentType assessmentType,
        Integer teacherClassSubjectId,
        String subjectName,
        String teacherName,
        Integer periodId,
        String periodName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
