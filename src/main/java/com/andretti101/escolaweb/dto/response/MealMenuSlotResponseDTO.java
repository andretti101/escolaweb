package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.entity.MealMenuSlot;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.MealPeriodEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record MealMenuSlotResponseDTO(
        Integer id,
        Integer academicYearId,
        Integer academicYear,
        DayOfWeekEnum dayOfWeek,
        String dayOfWeekLabel,
        MealPeriodEnum mealPeriod,
        String mealPeriodLabel,
        String mealPeriodDescription,
        String title,
        String description,
        String nutritionalNotes,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static MealMenuSlotResponseDTO fromEntity(MealMenuSlot slot) {
        if (slot == null) return null;
        return new MealMenuSlotResponseDTO(
                slot.getId(),
                slot.getAcademicYear() != null ? slot.getAcademicYear().getId() : null,
                slot.getAcademicYear() != null ? slot.getAcademicYear().getYear() : null,
                slot.getDayOfWeek(),
                slot.getDayOfWeek() != null ? slot.getDayOfWeek().getLabel() : null,
                slot.getMealPeriod(),
                slot.getMealPeriod() != null ? slot.getMealPeriod().getLabel() : null,
                slot.getMealPeriod() != null ? slot.getMealPeriod().getDescription() : null,
                slot.getTitle(),
                slot.getDescription(),
                slot.getNutritionalNotes(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }
}
