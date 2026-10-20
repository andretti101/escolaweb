package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;

import java.util.List;
import java.util.Map;

public record WeeklyScheduleDTO(
        Integer classRoomId,
        String classRoomName,
        Integer teacherId,
        String teacherName,
        List<TimetableSlotResponseDTO> slots,
        Map<DayOfWeekEnum, List<TimetableSlotResponseDTO>> slotsByDay
) {}
