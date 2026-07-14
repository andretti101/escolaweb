package com.andretti101.escolaweb.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EnrollmentResponseDTO(
        Integer id,
        Integer studentId,
        String studentName,
        Integer classRoomId,
        String classRoomName,
        Integer academicYear,
        LocalDate enrollmentDate,
        boolean active,
        LocalDateTime createdAt
) {}
