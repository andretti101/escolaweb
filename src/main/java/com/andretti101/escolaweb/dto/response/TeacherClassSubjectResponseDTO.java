package com.andretti101.escolaweb.dto.response;

import java.time.LocalDateTime;

public record TeacherClassSubjectResponseDTO(
        Integer id,
        Integer teacherId,
        String teacherName,
        Integer classRoomId,
        String classRoomName,
        Integer subjectId,
        String subjectName,
        Integer weeklyLessons,
        Integer minAssessmentsPerPeriod,
        Integer maxAssessmentsPerPeriod,
        boolean active,
        LocalDateTime createdAt
) {}
