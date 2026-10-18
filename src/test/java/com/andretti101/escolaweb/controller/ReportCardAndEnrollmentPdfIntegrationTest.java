package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportCardAndEnrollmentPdfIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private Student lucasAlmeida;
    private Student beatrizLopes;
    private AcademicYear activeYear;
    private Enrollment lucasEnrollment;
    private Enrollment beatrizEnrollment;

    @BeforeEach
    void setUp() {
        lucasAlmeida = studentRepository.findAll().stream()
                .filter(s -> "lucas.almeida@example.com".equals(s.getEmail()))
                .findFirst()
                .orElseThrow();

        beatrizLopes = studentRepository.findAll().stream()
                .filter(s -> "beatriz.lopes@example.com".equals(s.getEmail()))
                .findFirst()
                .orElseThrow();

        activeYear = academicYearRepository.findByActiveTrue().orElseThrow();

        lucasEnrollment = enrollmentRepository.findByStudent(lucasAlmeida).stream()
                .filter(Enrollment::isActive)
                .findFirst()
                .orElseThrow();

        beatrizEnrollment = enrollmentRepository.findByStudent(beatrizLopes).stream()
                .filter(Enrollment::isActive)
                .findFirst()
                .orElseThrow();
    }

    // ─────────────────────────────────────────────────────────────────
    // BOLETIM EM PDF (ReportCardController)
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/report-cards/student/{id}/year/{yId}/pdf: 401 when unauthenticated")
    void getReportCardPdf_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}/pdf",
                        lucasAlmeida.getId(), activeYear.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/report-cards/student/{id}/year/{yId}/pdf: 200 for STUDENT accessing own report card")
    @WithMockUser(username = "lucas.almeida@example.com", roles = "STUDENT")
    void getReportCardPdf_ownStudent_returns200AndValidPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}/pdf",
                        lucasAlmeida.getId(), activeYear.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"boletim-" + lucasAlmeida.getId() + ".pdf\""))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertNotNull(content);
        assertTrue(content.length > 500);
        String header = new String(content, 0, Math.min(content.length, 8), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("GET /api/report-cards/student/{id}/year/{yId}/pdf: 403 when STUDENT tries to access another student's report card")
    @WithMockUser(username = "lucas.almeida@example.com", roles = "STUDENT")
    void getReportCardPdf_otherStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}/pdf",
                        beatrizLopes.getId(), activeYear.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/report-cards/student/{id}/year/{yId}/pdf: 200 for SECRETARY accessing any student's report card")
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    void getReportCardPdf_secretary_returns200() throws Exception {
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}/pdf",
                        lucasAlmeida.getId(), activeYear.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("GET /api/report-cards/student/{id}/year/{yId}/pdf: 200 for PRINCIPAL accessing any student's report card")
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    void getReportCardPdf_principal_returns200() throws Exception {
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}/pdf",
                        beatrizLopes.getId(), activeYear.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    // ─────────────────────────────────────────────────────────────────
    // DECLARAÇÃO DE MATRÍCULA EM PDF (EnrollmentController)
    // ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate-pdf: 401 when unauthenticated")
    void getEnrollmentCertificatePdf_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/enrollments/{id}/certificate-pdf", lucasEnrollment.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate-pdf: 200 for STUDENT accessing own certificate")
    @WithMockUser(username = "lucas.almeida@example.com", roles = "STUDENT")
    void getEnrollmentCertificatePdf_ownStudent_returns200AndValidPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/enrollments/{id}/certificate-pdf", lucasEnrollment.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"declaracao-matricula-" + lucasAlmeida.getRegistrationNumber() + ".pdf\""))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertNotNull(content);
        assertTrue(content.length > 500);
        String header = new String(content, 0, Math.min(content.length, 8), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"));
    }

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate-pdf: 403 when STUDENT tries to access another student's certificate")
    @WithMockUser(username = "lucas.almeida@example.com", roles = "STUDENT")
    void getEnrollmentCertificatePdf_otherStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/enrollments/{id}/certificate-pdf", beatrizEnrollment.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate-pdf: 200 for SECRETARY")
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    void getEnrollmentCertificatePdf_secretary_returns200() throws Exception {
        mockMvc.perform(get("/api/enrollments/{id}/certificate-pdf", lucasEnrollment.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate-pdf: 200 for PRINCIPAL")
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    void getEnrollmentCertificatePdf_principal_returns200() throws Exception {
        mockMvc.perform(get("/api/enrollments/{id}/certificate-pdf", beatrizEnrollment.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("GET /api/report-cards/student/{id}/year/{yId}/pdf: 200 for TEACHER accessing student's report card")
    @WithMockUser(username = "mariana.ribeiro@example.com", roles = "TEACHER")
    void getReportCardPdf_teacher_returns200() throws Exception {
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}/pdf",
                        lucasAlmeida.getId(), activeYear.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    @DisplayName("GET /api/report-cards/student/{id}/year/{yId}/pdf: 404 when student not found")
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    void getReportCardPdf_studentNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}/pdf",
                        99999, activeYear.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate-pdf: 403 for TEACHER")
    @WithMockUser(username = "mariana.ribeiro@example.com", roles = "TEACHER")
    void getEnrollmentCertificatePdf_teacher_returns403() throws Exception {
        mockMvc.perform(get("/api/enrollments/{id}/certificate-pdf", lucasEnrollment.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/enrollments/{id}/certificate-pdf: 404 when enrollment not found")
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    void getEnrollmentCertificatePdf_enrollmentNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/enrollments/{id}/certificate-pdf", 99999))
                .andExpect(status().isNotFound());
    }
}
