package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.MealMenuSlot;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.MealPeriodEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealMenuSlotRepository extends JpaRepository<MealMenuSlot, Integer> {

    @Override
    @EntityGraph(attributePaths = {"academicYear"})
    Optional<MealMenuSlot> findById(Integer id);

    @EntityGraph(attributePaths = {"academicYear"})
    List<MealMenuSlot> findByAcademicYear_IdOrderByDayOfWeekAscMealPeriodAsc(Integer academicYearId);

    @EntityGraph(attributePaths = {"academicYear"})
    Optional<MealMenuSlot> findByAcademicYear_IdAndDayOfWeekAndMealPeriod(
            Integer academicYearId, DayOfWeekEnum dayOfWeek, MealPeriodEnum mealPeriod);

    boolean existsByAcademicYear_IdAndDayOfWeekAndMealPeriod(
            Integer academicYearId, DayOfWeekEnum dayOfWeek, MealPeriodEnum mealPeriod);

    boolean existsByAcademicYear_IdAndDayOfWeekAndMealPeriodAndIdNot(
            Integer academicYearId, DayOfWeekEnum dayOfWeek, MealPeriodEnum mealPeriod, Integer id);
}
