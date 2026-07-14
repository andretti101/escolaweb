package com.andretti101.escolaweb.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LessonResponseDTO(
        Integer id,
        LocalDate lessonDate,
        String content,
        String notes,
        Integer teacherClassSubjectId,
        String subjectName,
        String teacherName,
        String classRoomName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
