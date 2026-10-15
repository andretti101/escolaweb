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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/students with page=0&size=5 should return first 5 students and metadata")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testStudentsPagination() throws Exception {
        mockMvc.perform(get("/api/students")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[*].name", hasSize(greaterThanOrEqualTo(5))));
    }

    @Test
    @DisplayName("GET /api/students with out-of-bounds page should return empty page")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testStudentsPaginationOutOfBounds() throws Exception {
        mockMvc.perform(get("/api/students")
                        .param("page", "999")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.number", is(999)));
    }

    @Test
    @DisplayName("GET /api/classrooms with page=0&size=2 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testClassroomsPagination() throws Exception {
        mockMvc.perform(get("/api/classrooms")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/teachers with page=0&size=3 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testTeachersPagination() throws Exception {
        mockMvc.perform(get("/api/teachers")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.size", is(3)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("GET /api/subjects with page=0&size=4 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testSubjectsPagination() throws Exception {
        mockMvc.perform(get("/api/subjects")
                        .param("page", "0")
                        .param("size", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(4)))
                .andExpect(jsonPath("$.size", is(4)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(4)));
    }

    @Test
    @DisplayName("GET /api/academic-years with page=0&size=2 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testAcademicYearsPagination() throws Exception {
        mockMvc.perform(get("/api/academic-years")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.size", is(2)));
    }

    @Test
    @DisplayName("GET /api/academic-periods with page=0&size=2 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testAcademicPeriodsPagination() throws Exception {
        mockMvc.perform(get("/api/academic-periods")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.size", is(2)));
    }

    @Test
    @DisplayName("GET /api/enrollments with page=0&size=5 should paginate")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testEnrollmentsPagination() throws Exception {
        mockMvc.perform(get("/api/enrollments")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    @DisplayName("GET /api/teacher-class-subjects with page=0&size=3 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testTeacherClassSubjectsPagination() throws Exception {
        mockMvc.perform(get("/api/teacher-class-subjects")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(3))))
                .andExpect(jsonPath("$.size", is(3)));
    }

    @Test
    @DisplayName("GET /api/grades with page=0&size=5 should paginate")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGradesPagination() throws Exception {
        mockMvc.perform(get("/api/grades")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    @DisplayName("GET /api/attendances with page=0&size=5 should paginate")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testAttendancesPagination() throws Exception {
        mockMvc.perform(get("/api/attendances")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    @DisplayName("GET /api/lessons with page=0&size=5 should paginate")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testLessonsPagination() throws Exception {
        mockMvc.perform(get("/api/lessons")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    @DisplayName("GET /api/assessments with page=0&size=5 should paginate")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testAssessmentsPagination() throws Exception {
        mockMvc.perform(get("/api/assessments")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    @DisplayName("GET /api/principals with page=0&size=2 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testPrincipalsPagination() throws Exception {
        mockMvc.perform(get("/api/principals")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/secretaries with page=0&size=2 should paginate")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testSecretariesPagination() throws Exception {
        mockMvc.perform(get("/api/secretaries")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(2))))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/grade-history with page=0&size=5 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testGradeHistoryPagination() throws Exception {
        mockMvc.perform(get("/api/grade-history")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)));
    }

    @Test
    @DisplayName("GET /api/attendance-history with page=0&size=5 should paginate")
    @WithMockUser(username = "direcao@example.com", roles = {"PRINCIPAL"})
    void testAttendanceHistoryPagination() throws Exception {
        mockMvc.perform(get("/api/attendance-history")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)));
    }
}
