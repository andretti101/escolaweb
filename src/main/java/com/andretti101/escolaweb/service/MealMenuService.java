package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.MealMenuSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.MealMenuSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyMealMenuDTO;

public interface MealMenuService {

    WeeklyMealMenuDTO getWeeklyMenu(Integer academicYearId);

    MealMenuSlotResponseDTO getSlotById(Integer id);

    MealMenuSlotResponseDTO createSlot(MealMenuSlotRequestDTO dto);

    MealMenuSlotResponseDTO updateSlot(Integer id, MealMenuSlotRequestDTO dto);

    void deleteSlot(Integer id);
}
