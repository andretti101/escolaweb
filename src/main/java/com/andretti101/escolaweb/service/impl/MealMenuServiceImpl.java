package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.request.MealMenuSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.DayMealMenuDTO;
import com.andretti101.escolaweb.dto.response.MealMenuSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyMealMenuDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.MealMenuSlot;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.MealPeriodEnum;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.MealMenuSlotRepository;
import com.andretti101.escolaweb.service.MealMenuService;
import com.andretti101.escolaweb.service.exception.ConflictException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// Serviço de gestão do cardápio de alimentação escolar e merenda nutricional (PNAE)
@Slf4j
@Service
@RequiredArgsConstructor
public class MealMenuServiceImpl implements MealMenuService {

    private final MealMenuSlotRepository mealMenuSlotRepository;
    private final AcademicYearRepository academicYearRepository;

    // Constrói o cardápio da semana agrupando as refeições pelos dias letivos (segunda a sábado)
    @Override
    @Transactional(readOnly = true)
    public WeeklyMealMenuDTO getWeeklyMenu(Integer academicYearId) {
        AcademicYear year = resolveAcademicYear(academicYearId);
        List<MealMenuSlot> entities = mealMenuSlotRepository.findByAcademicYear_IdOrderByDayOfWeekAscMealPeriodAsc(year.getId());

        List<MealMenuSlotResponseDTO> slotDTOs = entities.stream()
                .sorted(Comparator.comparing(MealMenuSlot::getDayOfWeek).thenComparing(MealMenuSlot::getMealPeriod))
                .map(MealMenuSlotResponseDTO::fromEntity)
                .toList();

        Map<DayOfWeekEnum, Map<MealPeriodEnum, MealMenuSlotResponseDTO>> grouped = new EnumMap<>(DayOfWeekEnum.class);
        for (MealMenuSlotResponseDTO dto : slotDTOs) {
            grouped.computeIfAbsent(dto.dayOfWeek(), k -> new EnumMap<>(MealPeriodEnum.class))
                    .put(dto.mealPeriod(), dto);
        }

        boolean hasSaturday = grouped.containsKey(DayOfWeekEnum.SABADO);
        List<DayOfWeekEnum> daysToDisplay = new ArrayList<>(List.of(
                DayOfWeekEnum.SEGUNDA,
                DayOfWeekEnum.TERCA,
                DayOfWeekEnum.QUARTA,
                DayOfWeekEnum.QUINTA,
                DayOfWeekEnum.SEXTA
        ));
        if (hasSaturday) {
            daysToDisplay.add(DayOfWeekEnum.SABADO);
        }

        List<DayMealMenuDTO> daysList = new ArrayList<>();
        Map<DayOfWeekEnum, DayMealMenuDTO> daysMap = new LinkedHashMap<>();

        for (DayOfWeekEnum day : daysToDisplay) {
            Map<MealPeriodEnum, MealMenuSlotResponseDTO> dayMeals = grouped.getOrDefault(day, Collections.emptyMap());
            DayMealMenuDTO dayDTO = new DayMealMenuDTO(
                    day,
                    day.getLabel(),
                    dayMeals.get(MealPeriodEnum.MORNING),
                    dayMeals.get(MealPeriodEnum.LUNCH),
                    dayMeals.get(MealPeriodEnum.AFTERNOON)
            );
            daysList.add(dayDTO);
            daysMap.put(day, dayDTO);
        }

        return new WeeklyMealMenuDTO(
                year.getId(),
                year.getYear(),
                slotDTOs,
                daysList,
                daysMap
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MealMenuSlotResponseDTO getSlotById(Integer id) {
        MealMenuSlot slot = mealMenuSlotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cardápio não encontrado com o ID: " + id));
        return MealMenuSlotResponseDTO.fromEntity(slot);
    }

    @Override
    @Transactional
    public MealMenuSlotResponseDTO createSlot(MealMenuSlotRequestDTO dto) {
        AcademicYear year = resolveAcademicYear(dto.academicYearId());

        if (mealMenuSlotRepository.existsByAcademicYear_IdAndDayOfWeekAndMealPeriod(year.getId(), dto.dayOfWeek(), dto.mealPeriod())) {
            throw new ConflictException("Já existe uma refeição cadastrada para " + dto.dayOfWeek().getLabel() + " no período " + dto.mealPeriod().getLabel() + ".");
        }

        MealMenuSlot slot = MealMenuSlot.builder()
                .academicYear(year)
                .dayOfWeek(dto.dayOfWeek())
                .mealPeriod(dto.mealPeriod())
                .title(dto.title())
                .description(dto.description())
                .nutritionalNotes(dto.nutritionalNotes())
                .build();

        MealMenuSlot saved = mealMenuSlotRepository.save(slot);
        log.info("Meal menu slot created: id={}, year={}, day={}, period={}, title={}",
                saved.getId(), year.getYear(), dto.dayOfWeek(), dto.mealPeriod(), dto.title());

        return MealMenuSlotResponseDTO.fromEntity(saved);
    }

    @Override
    @Transactional
    public MealMenuSlotResponseDTO updateSlot(Integer id, MealMenuSlotRequestDTO dto) {
        MealMenuSlot slot = mealMenuSlotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cardápio não encontrado com o ID: " + id));

        AcademicYear year = dto.academicYearId() != null
                ? resolveAcademicYear(dto.academicYearId())
                : slot.getAcademicYear();

        if (mealMenuSlotRepository.existsByAcademicYear_IdAndDayOfWeekAndMealPeriodAndIdNot(year.getId(), dto.dayOfWeek(), dto.mealPeriod(), id)) {
            throw new ConflictException("Já existe uma refeição cadastrada para " + dto.dayOfWeek().getLabel() + " no período " + dto.mealPeriod().getLabel() + ".");
        }

        slot.setAcademicYear(year);
        slot.setDayOfWeek(dto.dayOfWeek());
        slot.setMealPeriod(dto.mealPeriod());
        slot.setTitle(dto.title());
        slot.setDescription(dto.description());
        slot.setNutritionalNotes(dto.nutritionalNotes());

        MealMenuSlot updated = mealMenuSlotRepository.save(slot);
        log.info("Meal menu slot updated: id={}, year={}, day={}, period={}, title={}",
                updated.getId(), year.getYear(), dto.dayOfWeek(), dto.mealPeriod(), dto.title());

        return MealMenuSlotResponseDTO.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSlot(Integer id) {
        MealMenuSlot slot = mealMenuSlotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cardápio não encontrado com o ID: " + id));
        mealMenuSlotRepository.delete(slot);
        log.info("Meal menu slot deleted: id={}", id);
    }

    private AcademicYear resolveAcademicYear(Integer academicYearId) {
        if (academicYearId != null) {
            return academicYearRepository.findById(academicYearId)
                    .orElseThrow(() -> new EntityNotFoundException("Ano letivo não encontrado com o ID: " + academicYearId));
        }
        return academicYearRepository.findByActiveTrue()
                .orElseThrow(() -> new EntityNotFoundException("Nenhum ano letivo ativo encontrado."));
    }
}
