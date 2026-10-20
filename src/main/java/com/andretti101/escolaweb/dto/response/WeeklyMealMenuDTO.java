package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;

import java.util.List;
import java.util.Map;

public record WeeklyMealMenuDTO(
        Integer academicYearId,
        Integer academicYear,
        List<MealMenuSlotResponseDTO> slots,
        List<DayMealMenuDTO> days,
        Map<DayOfWeekEnum, DayMealMenuDTO> daysByDay
) {}
