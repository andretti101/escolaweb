package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.StudentSituation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EnrollmentResponseDTO(
        Integer id,
        Integer studentId,
        String studentName,
        String studentRegistrationNumber,
        Integer classRoomId,
        String classRoomName,
        String schoolGrade,
        Integer academicYear,
        LocalDate enrollmentDate,
        boolean active,
        StudentSituation generalSituation,
        LocalDateTime createdAt
) {}
