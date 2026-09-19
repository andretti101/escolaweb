package com.andretti101.escolaweb.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementResponseDTO(
        Integer id,
        String title,
        String message,
        LocalDate eventDate,
        LocalDateTime createdAt,
        Integer authorId,
        String authorName,
        String authorRole,
        List<ClassRoomSimpleDTO> classRooms,
        boolean targetTeachers
) {}
