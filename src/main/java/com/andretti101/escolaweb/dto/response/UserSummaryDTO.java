package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public record UserSummaryDTO(
        Integer id,
        String name,
        String email,
        UserRole role,
        boolean active,
        LocalDateTime createdAt,
        String enrollmentNumber,
        String className,
        Integer classroomId,
        String academicYear,
        List<String> subjects,
        List<String> classrooms
) {
    public UserSummaryDTO {
        subjects = (subjects != null) ? subjects : List.of();
        classrooms = (classrooms != null) ? classrooms : List.of();
    }
}
