package com.andretti101.escolaweb.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GradeHistoryResponseDTO(
        Integer id,
        Integer gradeId,
        Integer studentId,
        String studentName,
        Integer classRoomId,
        String classRoomName,
        String subjectName,
        Integer teacherId,
        String teacherName,
        String assessmentTitle,
        String periodName,
        BigDecimal previousGrade,
        BigDecimal newGrade,
        String notes,
        String changedByName,
        LocalDateTime changedAt
) {}
