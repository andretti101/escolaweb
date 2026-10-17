package com.andretti101.escolaweb.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests running WITHOUT @Transactional on the test class.
 * This ensures HTTP requests and JSON serialization execute completely outside
 * the service transaction boundary, proving that open-in-view=false causes no
 * LazyInitializationException.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenInViewDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/classrooms serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetClassrooms() throws Exception {
        mockMvc.perform(get("/api/classrooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/students serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/students/{id} serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetStudentById() throws Exception {
        mockMvc.perform(get("/api/students/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.className", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/lessons serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetLessons() throws Exception {
        mockMvc.perform(get("/api/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/grades serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetGrades() throws Exception {
        mockMvc.perform(get("/api/grades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/attendances serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetAttendances() throws Exception {
        mockMvc.perform(get("/api/attendances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/announcements serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetAnnouncements() throws Exception {
        mockMvc.perform(get("/api/announcements"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/assessments serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetAssessments() throws Exception {
        mockMvc.perform(get("/api/assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/academic-periods serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetAcademicPeriods() throws Exception {
        mockMvc.perform(get("/api/academic-periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/teacher-class-subjects serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetTeacherClassSubjects() throws Exception {
        mockMvc.perform(get("/api/teacher-class-subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/teachers serializes without LazyInitializationException")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetTeachers() throws Exception {
        mockMvc.perform(get("/api/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }
}
