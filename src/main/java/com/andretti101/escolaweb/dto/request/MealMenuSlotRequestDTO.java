package com.andretti101.escolaweb.dto.request;

import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.MealPeriodEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MealMenuSlotRequestDTO(
        Integer academicYearId,

        @NotNull(message = "O dia da semana é obrigatório.")
        DayOfWeekEnum dayOfWeek,

        @NotNull(message = "O período da refeição é obrigatório.")
        MealPeriodEnum mealPeriod,

        @NotBlank(message = "O prato principal é obrigatório.")
        @Size(max = 150, message = "O título do prato deve ter no máximo 150 caracteres.")
        String title,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description,

        @Size(max = 255, message = "As observações nutricionais devem ter no máximo 255 caracteres.")
        String nutritionalNotes
) {
    public MealMenuSlotRequestDTO {
        title = title != null ? title.trim() : null;
        description = (description != null && !description.isBlank()) ? description.trim() : null;
        nutritionalNotes = (nutritionalNotes != null && !nutritionalNotes.isBlank()) ? nutritionalNotes.trim() : null;
    }
}
