package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.model.enums.UserRole;

import java.time.LocalDate;
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
        List<String> classrooms,
        StudentSituation generalSituation,
        String ownPhone,
        String guardianPhone,
        LocalDate birthDate
) {
    public UserSummaryDTO {
        subjects = (subjects != null) ? subjects : List.of();
        classrooms = (classrooms != null) ? classrooms : List.of();
    }

    public UserSummaryDTO(
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
        this(id, name, email, role, active, createdAt, enrollmentNumber, className, classroomId, academicYear, subjects, classrooms, null, null, null, null);
    }
}
