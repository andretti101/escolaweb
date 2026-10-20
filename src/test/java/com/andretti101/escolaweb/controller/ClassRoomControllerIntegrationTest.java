package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ClassRoomRequestDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClassRoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private AcademicPeriodService academicPeriodService;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Test
    @DisplayName("GET /api/classrooms without test transaction (reproducing open-in-view=false)")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetClassroomsWithoutTestTransaction() throws Exception {
        mockMvc.perform(get("/api/classrooms").param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/classrooms without test transaction (reproducing open-in-view=false)")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testCreateClassroomWithoutTestTransaction() throws Exception {
        AcademicYear activeYear = academicYearRepository.findByActiveTrue().orElseThrow();
        if (!academicPeriodRepository.existsByAcademicYearAndPeriodType(activeYear, AcademicPeriodType.TRIMESTER)) {
            academicPeriodService.createDivision(activeYear.getId(), AcademicPeriodType.TRIMESTER);
        }

        String uniqueName = "T-" + (System.currentTimeMillis() % 1000000);
        ClassRoomRequestDTO dto = new ClassRoomRequestDTO(
                activeYear.getId(),
                uniqueName,
                Shift.MORNING,
                AcademicPeriodType.TRIMESTER,
                SchoolGrade.FUNDAMENTAL_1
        );

        var result = mockMvc.perform(post("/api/classrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is(dto.name())))
                .andExpect(jsonPath("$.academicYear", is(activeYear.getYear())))
                .andReturn();

        Integer createdId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();

        try {
            // Verify GET /api/classrooms/{id}
            mockMvc.perform(get("/api/classrooms/" + createdId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(createdId)))
                    .andExpect(jsonPath("$.academicYear", is(activeYear.getYear())));

            // Verify GET /api/classrooms/academic-year/{yearId}
            mockMvc.perform(get("/api/classrooms/academic-year/" + activeYear.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", notNullValue()));

            // Verify PUT /api/classrooms/{id}/deactivate & /activate
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/classrooms/" + createdId + "/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(false)));

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/classrooms/" + createdId + "/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active", is(true)));
        } finally {
            classRoomRepository.deleteById(createdId);
        }
    }

    @Test
    @DisplayName("GET /api/academic-periods/divisions used by classroom creation form without test transaction")
    @WithMockUser(username = "secretaria@example.com", roles = {"SECRETARY"})
    void testGetDivisionsForClassroomCreationWithoutTestTransaction() throws Exception {
        AcademicYear activeYear = academicYearRepository.findByActiveTrue().orElseThrow();
        mockMvc.perform(get("/api/academic-periods/divisions").param("yearId", String.valueOf(activeYear.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }
}
