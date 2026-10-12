package com.andretti101.escolaweb.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceHistoryResponseDTO(
        Integer id,
        Integer attendanceId,
        Integer studentId,
        String studentName,
        Integer classRoomId,
        String classRoomName,
        String subjectName,
        Integer teacherId,
        String teacherName,
        LocalDate lessonDate,
        String previousStatus,
        String newStatus,
        String changedByName,
        LocalDateTime changedAt
) {}
