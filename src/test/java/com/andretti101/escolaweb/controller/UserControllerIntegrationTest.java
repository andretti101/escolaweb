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
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$.content[*].id", not(empty())))
                .andExpect(jsonPath("$.content[*].name", not(empty())))
                .andExpect(jsonPath("$.content[*].email", not(empty())))
                .andExpect(jsonPath("$.content[*].role", not(empty())));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/users como Secretaria com filtro de role=STUDENT retorna apenas alunos com turma e matrícula")
    void getUsers_filterStudent_returnsOnlyStudents() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].role", everyItem(is("STUDENT"))))
                .andExpect(jsonPath("$.content[0].enrollmentNumber", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/users com filtro de role=TEACHER retorna apenas professores com disciplinas")
    void getUsers_filterTeacher_returnsOnlyTeachers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].role", everyItem(is("TEACHER"))))
                .andExpect(jsonPath("$.content[0].subjects", notNullValue()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/users com filtro active=true retorna apenas usuários ativos")
    void getUsers_filterActive_returnsOnlyActiveUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].active", everyItem(is(true))));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/users com page=0&size=5 retorna página paginada com metadados")
    void getUsers_withPagination_returnsPagedResponse() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/users com página fora dos limites retorna conteúdo vazio")
    void getUsers_outOfBounds_returnsEmptyContent() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "999")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.number", is(999)));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/users/stats retorna métricas consolidadas dos usuários")
    void getUserStats_asPrincipal_returnsMetrics() throws Exception {
        mockMvc.perform(get("/api/users/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", greaterThan(0)))
                .andExpect(jsonPath("$.totalStaff", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalTeachers", greaterThan(0)))
                .andExpect(jsonPath("$.totalStudents", greaterThan(0)))
                .andExpect(jsonPath("$.studentsApproved", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.studentsPending", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.studentsFailed", greaterThanOrEqualTo(0)));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/users/stats com active=true retorna métricas de usuários ativos")
    void getUserStats_activeOnly_returnsMetrics() throws Exception {
        mockMvc.perform(get("/api/users/stats")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", greaterThan(0)))
                .andExpect(jsonPath("$.totalStudents", greaterThan(0)));
    }

    @Test
    @DisplayName("GET /api/users/stats sem autenticação retorna 401")
    void getUserStats_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/users/stats como Aluno retorna 403 Forbidden")
    void getUserStats_asStudent_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("GET /api/users com role=STAFF retorna Direção e Secretaria")
    void getUsers_withStaffRole_returnsPrincipalAndSecretary() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].role", everyItem(anyOf(is("PRINCIPAL"), is("SECRETARY")))));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("GET /api/users com múltiplos papéis role=PRINCIPAL,SECRETARY retorna ambos")
    void getUsers_withMultipleRoles_returnsBothRoles() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("role", "PRINCIPAL,SECRETARY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[*].role", everyItem(anyOf(is("PRINCIPAL"), is("SECRETARY")))));
    }
}
