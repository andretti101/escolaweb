package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.GradeLaunchRequestDTO;
import com.andretti101.escolaweb.dto.request.GradeUpdateRequestDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AssessmentType;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GradeHistoryNotesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private GradeHistoryRepository gradeHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Teacher teacher;
    private Student student;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        teacher = Teacher.builder()
                .name("Professor Auditoria Notas")
                .email("prof.auditoria.notas@escola.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);

        student = Student.builder()
                .name("Aluno Auditoria Notas")
                .email("aluno.auditoria.notas@escola.com")
                .password(passwordEncoder.encode("senha123"))
                .registrationNumber("MAT-AUD-999")
                .birthDate(LocalDate.of(2010, 4, 10))
                .role(UserRole.STUDENT)
                .active(true)
                .build();
        student = studentRepository.save(student);

        AcademicYear year = academicYearRepository.findAll().stream()
                .filter(AcademicYear::isActive)
                .findFirst()
                .orElseGet(() -> academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 20))
                        .active(true)
                        .build()));

        AcademicPeriod period = academicPeriodRepository.save(AcademicPeriod.builder()
                .name("1º Período Teste Auditoria")
                .periodType(com.andretti101.escolaweb.model.enums.AcademicPeriodType.TRIMESTER)
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 5, 30))
                .closed(false)
                .academicYear(year)
                .build());

        ClassRoom classRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Auditoria 9A")
                .shift(Shift.MORNING)
                .periodType(com.andretti101.escolaweb.model.enums.AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_2)
                .academicYear(year)
                .build());

        Subject subject = subjectRepository.save(Subject.builder()
                .name("Matemática Auditoria")
                .description("Disciplina de teste de auditoria")
                .active(true)
                .build());

        TeacherClassSubject tcs = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(teacher)
                .classRoom(classRoom)
                .subject(subject)
                .active(true)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .classRoom(classRoom)
                .enrollmentDate(LocalDate.of(2026, 2, 2))
                .active(true)
                .build());

        assessment = assessmentRepository.save(Assessment.builder()
                .title("Prova Mensal Auditoria")
                .date(LocalDate.of(2026, 3, 15))
                .assessmentType(AssessmentType.EXAM)
                .teacherClassSubject(tcs)
                .period(period)
                .active(true)
                .build());
    }

    @Test
    @WithMockUser(username = "prof.auditoria.notas@escola.com", roles = "TEACHER")
    @DisplayName("Only altered grades (PUT with changed value) are recorded in GradeHistory and returned in change-history")
    void launchAndUpdateGradeWithNotes_onlyAlteredGradesAppearInHistory() throws Exception {
        GradeLaunchRequestDTO launchDto = new GradeLaunchRequestDTO(
                student.getId(),
                assessment.getId(),
                new BigDecimal("6.5")
        );

        MvcResult launchRes = mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(launchDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(6.5))
                .andReturn();

        JsonNode createdGrade = objectMapper.readTree(launchRes.getResponse().getContentAsString());
        int gradeId = createdGrade.get("id").asInt();

        entityManager.flush();
        entityManager.clear();

        // Initial launch must NOT appear in change-history
        mockMvc.perform(get("/api/grade-history")
                        .param("studentId", String.valueOf(student.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        GradeUpdateRequestDTO updateDto = new GradeUpdateRequestDTO(
                new BigDecimal("8.0"),
                "Nota corrigida após revisão de questão discursiva"
        );

        mockMvc.perform(put("/api/grades/{id}", gradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(8.0));

        entityManager.flush();
        entityManager.clear();

        List<GradeHistory> timeline = gradeHistoryRepository.findTimelineByGradeId(gradeId);
        assertEquals(1, timeline.size());

        GradeHistory latest = timeline.get(0);
        assertEquals(0, new BigDecimal("6.5").compareTo(latest.getPreviousGrade()));
        assertEquals(0, new BigDecimal("8.0").compareTo(latest.getNewGrade()));
        assertEquals("Nota corrigida após revisão de questão discursiva", latest.getNotes());

        mockMvc.perform(get("/api/grade-history/grade/{gradeId}", gradeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].notes").value("Nota corrigida após revisão de questão discursiva"));

        mockMvc.perform(get("/api/grade-history")
                        .param("studentId", String.valueOf(student.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notes").value("Nota corrigida após revisão de questão discursiva"));

        mockMvc.perform(get("/api/grade-history")
                        .param("studentId", String.valueOf(student.getId()))
                        .param("periodId", String.valueOf(assessment.getPeriod().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/grade-history")
                        .param("studentId", String.valueOf(student.getId()))
                        .param("periodId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @WithMockUser(username = "prof.auditoria.notas@escola.com", roles = "TEACHER")
    @DisplayName("PUT /api/grades/{id} with unchanged numeric grade does not create history entry")
    void updateGrade_unchangedValueWithoutNotes_doesNotDuplicateHistory() throws Exception {
        GradeLaunchRequestDTO launchDto = new GradeLaunchRequestDTO(
                student.getId(),
                assessment.getId(),
                new BigDecimal("8.0")
        );

        MvcResult launchRes = mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(launchDto)))
                .andExpect(status().isCreated())
                .andReturn();

        int gradeId = objectMapper.readTree(launchRes.getResponse().getContentAsString()).get("id").asInt();
        entityManager.flush();
        entityManager.clear();

        // DB stores scale 2 (8.00); incoming JSON has scale 1 (8.0) and blank notes
        GradeUpdateRequestDTO unchangedDto = new GradeUpdateRequestDTO(
                new BigDecimal("8.0"),
                "   "
        );

        mockMvc.perform(put("/api/grades/{id}", gradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unchangedDto)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        List<GradeHistory> timeline = gradeHistoryRepository.findTimelineByGradeId(gradeId);
        assertEquals(0, timeline.size(), "Should not create history record when grade value is unchanged");
    }

    @Test
    @WithMockUser(username = "prof.auditoria.notas@escola.com", roles = "TEACHER")
    @DisplayName("Multiple alterations on the same grade all appear in order in change-history and timeline")
    void updateGrade_multipleAlterations_recordsAllAlterationsInOrder() throws Exception {
        GradeLaunchRequestDTO launchDto = new GradeLaunchRequestDTO(
                student.getId(),
                assessment.getId(),
                new BigDecimal("6.0")
        );

        MvcResult launchRes = mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(launchDto)))
                .andExpect(status().isCreated())
                .andReturn();

        int gradeId = objectMapper.readTree(launchRes.getResponse().getContentAsString()).get("id").asInt();
        entityManager.flush();
        entityManager.clear();

        GradeUpdateRequestDTO firstUpdate = new GradeUpdateRequestDTO(
                new BigDecimal("7.5"),
                "Primeira revisão"
        );
        mockMvc.perform(put("/api/grades/{id}", gradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpdate)))
                .andExpect(status().isOk());

        GradeUpdateRequestDTO secondUpdate = new GradeUpdateRequestDTO(
                new BigDecimal("9.0"),
                "Segunda revisão após recurso"
        );
        mockMvc.perform(put("/api/grades/{id}", gradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUpdate)))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        List<GradeHistory> timeline = gradeHistoryRepository.findTimelineByGradeId(gradeId);
        assertEquals(2, timeline.size());
        assertEquals("Segunda revisão após recurso", timeline.get(0).getNotes());
        assertEquals(0, new BigDecimal("7.5").compareTo(timeline.get(0).getPreviousGrade()));
        assertEquals(0, new BigDecimal("9.0").compareTo(timeline.get(0).getNewGrade()));
        assertEquals("Primeira revisão", timeline.get(1).getNotes());
        assertEquals(0, new BigDecimal("6.0").compareTo(timeline.get(1).getPreviousGrade()));
        assertEquals(0, new BigDecimal("7.5").compareTo(timeline.get(1).getNewGrade()));
    }

    @Test
    @WithMockUser(username = "secretaria.auditoria@escola.com", roles = "SECRETARY")
    @DisplayName("SECRETARY can update grade with justification notes via PUT /api/grades/{id}")
    void secretaryUpdateGradeWithNotes_persistsNotes() throws Exception {
        GradeLaunchRequestDTO launchDto = new GradeLaunchRequestDTO(
                student.getId(),
                assessment.getId(),
                new BigDecimal("5.5")
        );

        MvcResult launchRes = mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(launchDto)))
                .andExpect(status().isCreated())
                .andReturn();

        int gradeId = objectMapper.readTree(launchRes.getResponse().getContentAsString()).get("id").asInt();
        entityManager.flush();
        entityManager.clear();

        GradeUpdateRequestDTO updateDto = new GradeUpdateRequestDTO(
                new BigDecimal("7.0"),
                "Retificação solicitada pela coordenação pedagógica"
        );

        mockMvc.perform(put("/api/grades/{id}", gradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(7.0));

        entityManager.flush();
        entityManager.clear();

        List<GradeHistory> timeline = gradeHistoryRepository.findTimelineByGradeId(gradeId);
        assertEquals(1, timeline.size());
        assertEquals("Retificação solicitada pela coordenação pedagógica", timeline.get(0).getNotes());
    }

    @Test
    @WithMockUser(username = "prof.auditoria.notas@escola.com", roles = "TEACHER")
    @DisplayName("PUT /api/grades/{id} with notes over 500 characters fails with 400 Bad Request")
    void updateGrade_notesTooLong_returns400() throws Exception {
        GradeLaunchRequestDTO launchDto = new GradeLaunchRequestDTO(
                student.getId(),
                assessment.getId(),
                new BigDecimal("7.0")
        );

        MvcResult launchRes = mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(launchDto)))
                .andExpect(status().isCreated())
                .andReturn();

        int gradeId = objectMapper.readTree(launchRes.getResponse().getContentAsString()).get("id").asInt();

        String longNotes = "A".repeat(501);
        String payload = """
                {
                    "value": 9.0,
                    "notes": "%s"
                }
                """.formatted(longNotes);

        mockMvc.perform(put("/api/grades/{id}", gradeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
