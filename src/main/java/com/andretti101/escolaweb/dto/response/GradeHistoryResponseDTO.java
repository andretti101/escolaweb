package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.GradeKind;

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
        GradeKind gradeKind,
        BigDecimal previousGrade,
        BigDecimal newGrade,
        String notes,
        String changedByName,
        LocalDateTime changedAt
) {
    public GradeHistoryResponseDTO(
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
    ) {
        this(id, gradeId, studentId, studentName, classRoomId, classRoomName, subjectName,
                teacherId, teacherName, assessmentTitle, periodName, GradeKind.ORIGINAL,
                previousGrade, newGrade, notes, changedByName, changedAt);
    }
}
