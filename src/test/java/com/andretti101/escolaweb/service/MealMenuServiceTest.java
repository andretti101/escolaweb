package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.MealMenuSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.MealMenuSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyMealMenuDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.MealMenuSlot;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.MealPeriodEnum;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.MealMenuSlotRepository;
import com.andretti101.escolaweb.service.exception.ConflictException;
import com.andretti101.escolaweb.service.impl.MealMenuServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealMenuServiceTest {

    @Mock
    private MealMenuSlotRepository mealMenuSlotRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @InjectMocks
    private MealMenuServiceImpl mealMenuService;

    private AcademicYear activeYear;
    private MealMenuSlot slot1;
    private MealMenuSlot slot2;

    @BeforeEach
    void setUp() {
        activeYear = AcademicYear.builder()
                .id(3)
                .year(2026)
                .active(true)
                .build();

        slot1 = MealMenuSlot.builder()
                .id(1)
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .mealPeriod(MealPeriodEnum.MORNING)
                .title("Salada de Frutas")
                .description("Frutas frescas com iogurte")
                .nutritionalNotes("Sem glúten")
                .build();

        slot2 = MealMenuSlot.builder()
                .id(2)
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .mealPeriod(MealPeriodEnum.LUNCH)
                .title("Arroz e Feijão com Frango")
                .description("Acompanha salada verde")
                .nutritionalNotes("Rico em ferro")
                .build();
    }

    @Test
    @DisplayName("getWeeklyMenu: retorna cardápio da semana para o ano letivo ativo")
    void getWeeklyMenu_withActiveYear() {
        when(academicYearRepository.findByActiveTrue()).thenReturn(Optional.of(activeYear));
        when(mealMenuSlotRepository.findByAcademicYear_IdOrderByDayOfWeekAscMealPeriodAsc(3))
                .thenReturn(List.of(slot1, slot2));

        WeeklyMealMenuDTO result = mealMenuService.getWeeklyMenu(null);

        assertNotNull(result);
        assertEquals(3, result.academicYearId());
        assertEquals(2026, result.academicYear());
        assertEquals(2, result.slots().size());
        assertEquals(5, result.days().size());

        var monday = result.daysByDay().get(DayOfWeekEnum.SEGUNDA);
        assertNotNull(monday);
        assertNotNull(monday.morning());
        assertEquals("Salada de Frutas", monday.morning().title());
        assertNotNull(monday.lunch());
        assertEquals("Arroz e Feijão com Frango", monday.lunch().title());
        assertNull(monday.afternoon());
    }

    @Test
    @DisplayName("getWeeklyMenu: retorna cardápio para ano letivo específico informado")
    void getWeeklyMenu_withSpecificYear() {
        AcademicYear year2025 = AcademicYear.builder().id(2).year(2025).active(false).build();
        when(academicYearRepository.findById(2)).thenReturn(Optional.of(year2025));
        when(mealMenuSlotRepository.findByAcademicYear_IdOrderByDayOfWeekAscMealPeriodAsc(2))
                .thenReturn(List.of());

        WeeklyMealMenuDTO result = mealMenuService.getWeeklyMenu(2);

        assertNotNull(result);
        assertEquals(2, result.academicYearId());
        assertEquals(2025, result.academicYear());
        assertTrue(result.slots().isEmpty());
    }

    @Test
    @DisplayName("getWeeklyMenu: lança EntityNotFoundException quando não há ano letivo ativo")
    void getWeeklyMenu_noActiveYear_throwsException() {
        when(academicYearRepository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mealMenuService.getWeeklyMenu(null));
    }

    @Test
    @DisplayName("getWeeklyMenu: inclui Sábado quando houver refeição cadastrada no sábado")
    void getWeeklyMenu_includesSaturdayWhenPresent() {
        MealMenuSlot saturdaySlot = MealMenuSlot.builder()
                .id(10)
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.MORNING)
                .title("Lanche Especial de Sábado")
                .build();

        when(academicYearRepository.findByActiveTrue()).thenReturn(Optional.of(activeYear));
        when(mealMenuSlotRepository.findByAcademicYear_IdOrderByDayOfWeekAscMealPeriodAsc(3))
                .thenReturn(List.of(slot1, saturdaySlot));

        WeeklyMealMenuDTO result = mealMenuService.getWeeklyMenu(null);

        assertNotNull(result);
        assertEquals(6, result.days().size());
        assertTrue(result.daysByDay().containsKey(DayOfWeekEnum.SABADO));
        assertNotNull(result.daysByDay().get(DayOfWeekEnum.SABADO).morning());
    }

    @Test
    @DisplayName("getSlotById: retorna refeição existente com sucesso")
    void getSlotById_success() {
        when(mealMenuSlotRepository.findById(1)).thenReturn(Optional.of(slot1));

        MealMenuSlotResponseDTO dto = mealMenuService.getSlotById(1);

        assertNotNull(dto);
        assertEquals(1, dto.id());
        assertEquals("Salada de Frutas", dto.title());
        assertEquals(DayOfWeekEnum.SEGUNDA, dto.dayOfWeek());
        assertEquals(MealPeriodEnum.MORNING, dto.mealPeriod());
    }

    @Test
    @DisplayName("getSlotById: lança EntityNotFoundException quando ID não existe")
    void getSlotById_notFound() {
        when(mealMenuSlotRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mealMenuService.getSlotById(999));
    }

    @Test
    @DisplayName("createSlot: cria refeição com sucesso quando não há conflito")
    void createSlot_success() {
        MealMenuSlotRequestDTO request = new MealMenuSlotRequestDTO(
                null,
                DayOfWeekEnum.TERCA,
                MealPeriodEnum.LUNCH,
                "Carne Moída com Purê",
                "Acompanha salada",
                "Rico em ferro"
        );

        when(academicYearRepository.findByActiveTrue()).thenReturn(Optional.of(activeYear));
        when(mealMenuSlotRepository.existsByAcademicYear_IdAndDayOfWeekAndMealPeriod(3, DayOfWeekEnum.TERCA, MealPeriodEnum.LUNCH))
                .thenReturn(false);

        MealMenuSlot savedSlot = MealMenuSlot.builder()
                .id(5)
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.TERCA)
                .mealPeriod(MealPeriodEnum.LUNCH)
                .title("Carne Moída com Purê")
                .description("Acompanha salada")
                .nutritionalNotes("Rico em ferro")
                .build();

        when(mealMenuSlotRepository.save(any(MealMenuSlot.class))).thenReturn(savedSlot);

        MealMenuSlotResponseDTO response = mealMenuService.createSlot(request);

        assertNotNull(response);
        assertEquals(5, response.id());
        assertEquals("Carne Moída com Purê", response.title());
        assertEquals(DayOfWeekEnum.TERCA, response.dayOfWeek());
        assertEquals(MealPeriodEnum.LUNCH, response.mealPeriod());
        verify(mealMenuSlotRepository).save(any(MealMenuSlot.class));
    }

    @Test
    @DisplayName("createSlot: lança ConflictException quando já existe refeição no mesmo dia e período")
    void createSlot_conflict_throwsConflictException() {
        MealMenuSlotRequestDTO request = new MealMenuSlotRequestDTO(
                3,
                DayOfWeekEnum.SEGUNDA,
                MealPeriodEnum.MORNING,
                "Outra Salada de Frutas",
                null,
                null
        );

        when(academicYearRepository.findById(3)).thenReturn(Optional.of(activeYear));
        when(mealMenuSlotRepository.existsByAcademicYear_IdAndDayOfWeekAndMealPeriod(3, DayOfWeekEnum.SEGUNDA, MealPeriodEnum.MORNING))
                .thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> mealMenuService.createSlot(request));
        assertTrue(ex.getMessage().contains("Já existe uma refeição cadastrada"));
        verify(mealMenuSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSlot: atualiza refeição existente com sucesso")
    void updateSlot_success() {
        MealMenuSlotRequestDTO updateRequest = new MealMenuSlotRequestDTO(
                null,
                DayOfWeekEnum.SEGUNDA,
                MealPeriodEnum.MORNING,
                "Salada de Frutas Especial",
                "Com mel e aveia",
                "Sem lactose"
        );

        when(mealMenuSlotRepository.findById(1)).thenReturn(Optional.of(slot1));
        when(mealMenuSlotRepository.existsByAcademicYear_IdAndDayOfWeekAndMealPeriodAndIdNot(
                3, DayOfWeekEnum.SEGUNDA, MealPeriodEnum.MORNING, 1)).thenReturn(false);
        when(mealMenuSlotRepository.save(any(MealMenuSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MealMenuSlotResponseDTO updated = mealMenuService.updateSlot(1, updateRequest);

        assertNotNull(updated);
        assertEquals("Salada de Frutas Especial", updated.title());
        assertEquals("Com mel e aveia", updated.description());
        assertEquals("Sem lactose", updated.nutritionalNotes());
    }

    @Test
    @DisplayName("updateSlot: lança ConflictException quando alterado para dia e período já ocupados por outro slot")
    void updateSlot_conflict_throwsConflictException() {
        MealMenuSlotRequestDTO updateRequest = new MealMenuSlotRequestDTO(
                null,
                DayOfWeekEnum.SEGUNDA,
                MealPeriodEnum.LUNCH,
                "Tentativa de sobrescrever almoço",
                null,
                null
        );

        when(mealMenuSlotRepository.findById(1)).thenReturn(Optional.of(slot1));
        when(mealMenuSlotRepository.existsByAcademicYear_IdAndDayOfWeekAndMealPeriodAndIdNot(
                3, DayOfWeekEnum.SEGUNDA, MealPeriodEnum.LUNCH, 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> mealMenuService.updateSlot(1, updateRequest));
        verify(mealMenuSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSlot: lança EntityNotFoundException quando id não existe")
    void updateSlot_notFound() {
        MealMenuSlotRequestDTO request = new MealMenuSlotRequestDTO(
                null,
                DayOfWeekEnum.SEGUNDA,
                MealPeriodEnum.MORNING,
                "Qualquer",
                null,
                null
        );
        when(mealMenuSlotRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mealMenuService.updateSlot(99, request));
    }

    @Test
    @DisplayName("deleteSlot: remove refeição existente com sucesso")
    void deleteSlot_success() {
        when(mealMenuSlotRepository.findById(1)).thenReturn(Optional.of(slot1));

        assertDoesNotThrow(() -> mealMenuService.deleteSlot(1));
        verify(mealMenuSlotRepository).delete(slot1);
    }

    @Test
    @DisplayName("deleteSlot: lança EntityNotFoundException quando refeição não existe")
    void deleteSlot_notFound() {
        when(mealMenuSlotRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> mealMenuService.deleteSlot(999));
        verify(mealMenuSlotRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getWeeklyMenu: ordena slots cronologicamente por dia da semana e período de refeição")
    void getWeeklyMenu_sortsSlotsChronologically() {
        // Entidades passadas propositalmente fora de ordem cronológica (ordem alfabética SQL)
        MealMenuSlot quartaAfternoon = MealMenuSlot.builder().id(9).academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.QUARTA).mealPeriod(MealPeriodEnum.AFTERNOON).title("Quarta Tarde").build();
        MealMenuSlot segundaMorning = MealMenuSlot.builder().id(1).academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA).mealPeriod(MealPeriodEnum.MORNING).title("Segunda Manhã").build();
        MealMenuSlot segundaLunch = MealMenuSlot.builder().id(2).academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA).mealPeriod(MealPeriodEnum.LUNCH).title("Segunda Almoço").build();
        MealMenuSlot tercaMorning = MealMenuSlot.builder().id(4).academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.TERCA).mealPeriod(MealPeriodEnum.MORNING).title("Terça Manhã").build();

        when(academicYearRepository.findByActiveTrue()).thenReturn(Optional.of(activeYear));
        when(mealMenuSlotRepository.findByAcademicYear_IdOrderByDayOfWeekAscMealPeriodAsc(3))
                .thenReturn(List.of(quartaAfternoon, segundaLunch, tercaMorning, segundaMorning));

        WeeklyMealMenuDTO result = mealMenuService.getWeeklyMenu(null);

        assertNotNull(result);
        assertEquals(4, result.slots().size());
        assertEquals("Segunda Manhã", result.slots().get(0).title());
        assertEquals("Segunda Almoço", result.slots().get(1).title());
        assertEquals("Terça Manhã", result.slots().get(2).title());
        assertEquals("Quarta Tarde", result.slots().get(3).title());
    }
}
