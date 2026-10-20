package com.andretti101.escolaweb.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/users sem autenticação retorna 401 ou 403")
    void getUsers_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/users como Aluno retorna 403 Forbidden")
    void getUsers_asStudent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("GET /api/users como Professor retorna 403 Forbidden")
    void getUsers_asTeacher_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/users como Direção retorna 200 com lista completa consolidada")
    void getUsers_asPrincipal_returnsConsolidatedList() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].id", not(empty())))
                .andExpect(jsonPath("$[*].name", not(empty())))
                .andExpect(jsonPath("$[*].email", not(empty())))
                .andExpect(jsonPath("$[*].role", not(empty())));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/users como Secretaria com filtro de role=STUDENT retorna apenas alunos com turma e matrícula")
    void getUsers_filterStudent_returnsOnlyStudents() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].role", everyItem(is("STUDENT"))))
                .andExpect(jsonPath("$[0].enrollmentNumber", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/users com filtro de role=TEACHER retorna apenas professores com disciplinas")
    void getUsers_filterTeacher_returnsOnlyTeachers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].role", everyItem(is("TEACHER"))))
                .andExpect(jsonPath("$[0].subjects", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/users com filtro active=true retorna apenas usuários ativos")
    void getUsers_filterActive_returnsOnlyActiveUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].active", everyItem(is(true))));
    }
}
