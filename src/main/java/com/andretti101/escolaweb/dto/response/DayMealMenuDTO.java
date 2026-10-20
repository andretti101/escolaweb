package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;

public record DayMealMenuDTO(
        DayOfWeekEnum dayOfWeek,
        String dayOfWeekLabel,
        MealMenuSlotResponseDTO morning,
        MealMenuSlotResponseDTO lunch,
        MealMenuSlotResponseDTO afternoon
) {}
