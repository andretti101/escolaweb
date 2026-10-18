package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.SchoolSettingsRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SchoolSettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/school-settings: 401 when unauthenticated")
    void getSettings_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/school-settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/school-settings: 200 for STUDENT")
    @WithMockUser(username = "aluno@example.com", roles = "STUDENT")
    void getSettings_student_returns200() throws Exception {
        mockMvc.perform(get("/api/school-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").exists());
    }

    @Test
    @DisplayName("GET /api/school-settings: 200 for TEACHER")
    @WithMockUser(username = "prof@example.com", roles = "TEACHER")
    void getSettings_teacher_returns200() throws Exception {
        mockMvc.perform(get("/api/school-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").exists());
    }

    @Test
    @DisplayName("PUT /api/school-settings: 403 for TEACHER")
    @WithMockUser(username = "prof@example.com", roles = "TEACHER")
    void updateSettings_teacher_returns403() throws Exception {
        SchoolSettingsRequestDTO dto = new SchoolSettingsRequestDTO(
                "Nova Escola",
                "88801-010",
                "Rua Teste, 100",
                "(48) 3333-1111",
                "contato@teste.com",
                new BigDecimal("6.0"),
                new BigDecimal("75.0")
        );

        mockMvc.perform(put("/api/school-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/school-settings: updates cep and address successfully for PRINCIPAL")
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    void updateSettings_principal_updatesCepAndAddress() throws Exception {
        SchoolSettingsRequestDTO dto = new SchoolSettingsRequestDTO(
                "Escola Central Integrada",
                "01310-100",
                "Avenida Paulista, 1000 - Bela Vista, São Paulo - SP",
                "(11) 3200-0000",
                "direcao@escolacentral.edu.br",
                new BigDecimal("7.0"),
                new BigDecimal("80.0")
        );

        mockMvc.perform(put("/api/school-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value("Escola Central Integrada"))
                .andExpect(jsonPath("$.cep").value("01310-100"))
                .andExpect(jsonPath("$.address").value("Avenida Paulista, 1000 - Bela Vista, São Paulo - SP"))
                .andExpect(jsonPath("$.phone").value("(11) 3200-0000"))
                .andExpect(jsonPath("$.email").value("direcao@escolacentral.edu.br"));

        // Verify with GET that changes persisted
        mockMvc.perform(get("/api/school-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value("Escola Central Integrada"))
                .andExpect(jsonPath("$.cep").value("01310-100"))
                .andExpect(jsonPath("$.address").value("Avenida Paulista, 1000 - Bela Vista, São Paulo - SP"));
    }

    @Test
    @DisplayName("PUT /api/school-settings: 400 when cep exceeds 20 characters")
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    void updateSettings_invalidCep_returns400() throws Exception {
        SchoolSettingsRequestDTO dto = new SchoolSettingsRequestDTO(
                "Escola Teste",
                "123456789012345678901", // 21 chars
                "Rua Teste, 100",
                "(48) 3333-1111",
                "teste@escola.com",
                new BigDecimal("6.0"),
                new BigDecimal("75.0")
        );

        mockMvc.perform(put("/api/school-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
