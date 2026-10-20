package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.MealMenuSlotRequestDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.MealMenuSlot;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.MealPeriodEnum;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.MealMenuSlotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MealMenuControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private MealMenuSlotRepository mealMenuSlotRepository;

    private AcademicYear activeYear;

    @BeforeEach
    void setUp() {
        activeYear = academicYearRepository.findByActiveTrue()
                .orElseGet(() -> academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .active(true)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 15))
                        .periodType(AcademicPeriodType.TRIMESTER)
                        .build()));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    @DisplayName("Direção: POST /api/menu cria refeição com sucesso (201 Created)")
    void principalCanCreateMealMenuSlot() throws Exception {
        MealMenuSlotRequestDTO request = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.MORNING,
                "Lanche Especial de Sábado",
                "Suco natural e bolo de aveia",
                "Sem glúten"
        );

        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Lanche Especial de Sábado"))
                .andExpect(jsonPath("$.dayOfWeek").value("SABADO"))
                .andExpect(jsonPath("$.mealPeriod").value("MORNING"))
                .andExpect(jsonPath("$.description").value("Suco natural e bolo de aveia"))
                .andExpect(jsonPath("$.nutritionalNotes").value("Sem glúten"));
    }

    @Test
    @WithMockUser(username = "lucas.almeida@example.com", roles = {"STUDENT"})
    @DisplayName("Aluno: POST /api/menu é proibido (403 Forbidden)")
    void studentCannotCreateMealMenuSlot() throws Exception {
        MealMenuSlotRequestDTO request = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.LUNCH,
                "Tentativa Aluno",
                null,
                null
        );

        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mariana.ribeiro@example.com", roles = {"TEACHER"})
    @DisplayName("Professor: POST /api/menu é proibido (403 Forbidden)")
    void teacherCannotCreateMealMenuSlot() throws Exception {
        MealMenuSlotRequestDTO request = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.AFTERNOON,
                "Tentativa Professor",
                null,
                null
        );

        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    @DisplayName("Secretaria: POST /api/menu é proibido (403 Forbidden)")
    void secretaryCannotCreateMealMenuSlot() throws Exception {
        MealMenuSlotRequestDTO request = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.AFTERNOON,
                "Tentativa Secretaria",
                null,
                null
        );

        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    @DisplayName("Direção: PUT /api/menu/{id} atualiza refeição existente (200 OK)")
    void principalCanUpdateMealMenuSlot() throws Exception {
        MealMenuSlot slot = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.LUNCH)
                .title("Prato Original")
                .description("Descrição Original")
                .build());

        MealMenuSlotRequestDTO updateRequest = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.LUNCH,
                "Prato Atualizado com Sucesso",
                "Nova Descrição Completa",
                "Rico em vitaminas"
        );

        mockMvc.perform(put("/api/menu/" + slot.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slot.getId()))
                .andExpect(jsonPath("$.title").value("Prato Atualizado com Sucesso"))
                .andExpect(jsonPath("$.nutritionalNotes").value("Rico em vitaminas"));
    }

    @Test
    @WithMockUser(username = "lucas.almeida@example.com", roles = {"STUDENT"})
    @DisplayName("Aluno: PUT /api/menu/{id} é proibido (403 Forbidden)")
    void studentCannotUpdateMealMenuSlot() throws Exception {
        MealMenuSlot slot = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.LUNCH)
                .title("Prato Teste")
                .build());

        MealMenuSlotRequestDTO updateRequest = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.LUNCH,
                "Alteração Proibida",
                null,
                null
        );

        mockMvc.perform(put("/api/menu/" + slot.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    @DisplayName("Direção: DELETE /api/menu/{id} remove refeição (204 No Content)")
    void principalCanDeleteMealMenuSlot() throws Exception {
        MealMenuSlot slot = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.AFTERNOON)
                .title("Lanche Para Excluir")
                .build());

        mockMvc.perform(delete("/api/menu/" + slot.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/menu/" + slot.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "lucas.almeida@example.com", roles = {"STUDENT"})
    @DisplayName("Aluno: DELETE /api/menu/{id} é proibido (403 Forbidden)")
    void studentCannotDeleteMealMenuSlot() throws Exception {
        MealMenuSlot slot = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.AFTERNOON)
                .title("Lanche Protegido")
                .build());

        mockMvc.perform(delete("/api/menu/" + slot.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "lucas.almeida@example.com", roles = {"STUDENT"})
    @DisplayName("Aluno: GET /api/menu/weekly acessa cardápio semanal com sucesso (200 OK)")
    void studentCanReadWeeklyMenu() throws Exception {
        mockMvc.perform(get("/api/menu/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicYearId").isNumber())
                .andExpect(jsonPath("$.days").isArray());
    }

    @Test
    @WithMockUser(username = "mariana.ribeiro@example.com", roles = {"TEACHER"})
    @DisplayName("Professor: GET /api/menu/weekly acessa cardápio semanal (200 OK)")
    void teacherCanReadWeeklyMenu() throws Exception {
        mockMvc.perform(get("/api/menu/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicYear").value(2026));
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    @DisplayName("Secretaria: GET /api/menu/weekly acessa cardápio semanal (200 OK)")
    void secretaryCanReadWeeklyMenu() throws Exception {
        mockMvc.perform(get("/api/menu/weekly"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    @DisplayName("Direção: GET /api/menu/weekly acessa cardápio semanal (200 OK)")
    void principalCanReadWeeklyMenu() throws Exception {
        mockMvc.perform(get("/api/menu/weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("Anônimo: GET /api/menu/weekly sem autenticação retorna 401 ou 403")
    void anonymousCannotAccessWeeklyMenu() throws Exception {
        mockMvc.perform(get("/api/menu/weekly"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"PRINCIPAL"})
    @DisplayName("Conflito: POST /api/menu com dia e período já existentes retorna 409 Conflict")
    void createMealMenuSlot_conflictReturns409() throws Exception {
        mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.MORNING)
                .title("Café Existente")
                .build());

        MealMenuSlotRequestDTO duplicateRequest = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.MORNING,
                "Novo Café Conflitante",
                null,
                null
        );

        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Já existe uma refeição cadastrada")));
    }

    @Test
    @WithMockUser(roles = {"PRINCIPAL"})
    @DisplayName("Conflito: PUT /api/menu/{id} tentando mudar dia/período para slot já ocupado retorna 409 Conflict")
    void updateMealMenuSlot_conflictReturns409() throws Exception {
        MealMenuSlot slotMorning = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.MORNING)
                .title("Café")
                .build());

        MealMenuSlot slotLunch = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.LUNCH)
                .title("Almoço")
                .build());

        // Tentar atualizar o almoço para manhã (onde já existe slotMorning)
        MealMenuSlotRequestDTO conflictRequest = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.MORNING,
                "Tentando mover almoço para manhã",
                null,
                null
        );

        mockMvc.perform(put("/api/menu/" + slotLunch.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = {"STUDENT"})
    @DisplayName("404: GET /api/menu/999999 retorna 404 Not Found")
    void getSlotById_notFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/menu/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"PRINCIPAL"})
    @DisplayName("400: POST /api/menu com título em branco retorna 400 Bad Request")
    void createMealMenuSlot_invalidDtoReturns400() throws Exception {
        String invalidJson = """
            {
                "dayOfWeek": "SEGUNDA",
                "mealPeriod": "LUNCH",
                "title": "   "
            }
        """;

        mockMvc.perform(post("/api/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lucas.almeida@example.com", roles = {"STUDENT"})
    @DisplayName("200 OK: GET /api/menu/{id} retorna os detalhes da refeição com todos os campos")
    void getSlotById_returns200WithFullDetails() throws Exception {
        MealMenuSlot slot = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.LUNCH)
                .title("Moqueca de Peixe")
                .description("Acompanha arroz de coco e pirão")
                .nutritionalNotes("Rico em fósforo e ômega 3")
                .build());

        mockMvc.perform(get("/api/menu/" + slot.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slot.getId()))
                .andExpect(jsonPath("$.academicYearId").value(activeYear.getId()))
                .andExpect(jsonPath("$.academicYear").value(activeYear.getYear()))
                .andExpect(jsonPath("$.dayOfWeek").value("SABADO"))
                .andExpect(jsonPath("$.dayOfWeekLabel").value("Sábado"))
                .andExpect(jsonPath("$.mealPeriod").value("LUNCH"))
                .andExpect(jsonPath("$.mealPeriodLabel").value("Almoço"))
                .andExpect(jsonPath("$.title").value("Moqueca de Peixe"))
                .andExpect(jsonPath("$.description").value("Acompanha arroz de coco e pirão"))
                .andExpect(jsonPath("$.nutritionalNotes").value("Rico em fósforo e ômega 3"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "mariana.ribeiro@example.com", roles = {"TEACHER"})
    @DisplayName("Professor: PUT /api/menu/{id} é proibido (403 Forbidden)")
    void teacherCannotUpdateMealMenuSlot() throws Exception {
        MealMenuSlot slot = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.MORNING)
                .title("Café Professor")
                .build());

        MealMenuSlotRequestDTO updateRequest = new MealMenuSlotRequestDTO(
                activeYear.getId(),
                DayOfWeekEnum.SABADO,
                MealPeriodEnum.MORNING,
                "Tentativa Professor",
                null,
                null
        );

        mockMvc.perform(put("/api/menu/" + slot.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    @DisplayName("Secretaria: DELETE /api/menu/{id} é proibido (403 Forbidden)")
    void secretaryCannotDeleteMealMenuSlot() throws Exception {
        MealMenuSlot slot = mealMenuSlotRepository.save(MealMenuSlot.builder()
                .academicYear(activeYear)
                .dayOfWeek(DayOfWeekEnum.SABADO)
                .mealPeriod(MealPeriodEnum.MORNING)
                .title("Café Secretaria")
                .build());

        mockMvc.perform(delete("/api/menu/" + slot.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    @DisplayName("Direção: GET /api/menu/weekly?academicYearId=... retorna cardápio filtrado por ano letivo")
    void getWeeklyMenu_withExplicitAcademicYearId() throws Exception {
        mockMvc.perform(get("/api/menu/weekly")
                        .param("academicYearId", String.valueOf(activeYear.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicYearId").value(activeYear.getId()))
                .andExpect(jsonPath("$.academicYear").value(activeYear.getYear()))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.slots").isArray());
    }
}
