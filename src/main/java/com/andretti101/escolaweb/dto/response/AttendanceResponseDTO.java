package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponseDTO(
        Integer id,
        Integer studentId,
        String studentName,
        Integer lessonId,
        LocalDate lessonDate,
        AttendanceStatus status,
        String notes,
        LocalDateTime createdAt
) {}
