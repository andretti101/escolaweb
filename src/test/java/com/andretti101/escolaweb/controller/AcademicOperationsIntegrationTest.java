package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AssessmentRequestDTO;
import com.andretti101.escolaweb.dto.request.AttendanceRequestDTO;
import com.andretti101.escolaweb.dto.request.AttendanceUpdateRequestDTO;
import com.andretti101.escolaweb.dto.request.GradeLaunchRequestDTO;
import com.andretti101.escolaweb.dto.request.LessonRequestDTO;
import com.andretti101.escolaweb.dto.request.SchoolSettingsRequestDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.*;
import com.andretti101.escolaweb.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AcademicOperationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AcademicYear activeYear;
    private AcademicPeriod openPeriod;
    private ClassRoom classRoom;
    private Subject subject;
    private Teacher ownerTeacher;
    private Teacher otherTeacher;
    private Student enrolledStudent;
    private Student otherStudent;
    private TeacherClassSubject tcs;

    @BeforeEach
    void setUp() {
        activeYear = academicYearRepository.findByActiveTrue().orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 15))
                        .active(true)
                        .build()));

        openPeriod = academicPeriodRepository.save(AcademicPeriod.builder()
                .name("1º Trimestre Ops")
                .periodType(AcademicPeriodType.TRIMESTER)
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 5, 30))
                .closed(false)
                .academicYear(activeYear)
                .build());

        subject = subjectRepository.save(Subject.builder()
                .name("Química Experimental Ops")
                .description("Disciplina de teste de operações acadêmicas")
                .active(true)
                .build());

        ownerTeacher = teacherRepository.save(Teacher.builder()
                .name("Prof Owner Ops")
                .email("prof.owner.ops@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .subjects(Set.of(subject))
                .active(true)
                .build());

        otherTeacher = teacherRepository.save(Teacher.builder()
                .name("Prof Other Ops")
                .email("prof.other.ops@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .subjects(Set.of(subject))
                .active(true)
                .build());

        enrolledStudent = studentRepository.save(Student.builder()
                .name("Aluno Matriculado Ops")
                .email("aluno.enrolled.ops@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .registrationNumber("2026OPS01")
                .birthDate(LocalDate.of(2010, 3, 20))
                .guardianPhone("(48) 99999-2222")
                .active(true)
                .build());

        otherStudent = studentRepository.save(Student.builder()
                .name("Outro Aluno Ops")
                .email("aluno.other.ops@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .registrationNumber("2026OPS02")
                .birthDate(LocalDate.of(2010, 7, 11))
                .guardianPhone("(48) 99999-3333")
                .active(true)
                .build());

        classRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Ops 1A")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(enrolledStudent)
                .classRoom(classRoom)
                .enrollmentDate(LocalDate.of(2026, 2, 2))
                .active(true)
                .build());

        tcs = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(ownerTeacher)
                .classRoom(classRoom)
                .subject(subject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(2)
                .active(true)
                .build());
    }

    @Test
    @WithMockUser(username = "prof.owner.ops@example.com", roles = "TEACHER")
    @DisplayName("Fluxo completo do professor titular: cria aula, registra e atualiza presença, cria avaliação, lança nota e consulta média e frequência")
    void teacherOwnerFullAcademicFlow_succeeds() throws Exception {
        // 1. Criar aula (2 tempos)
        LessonRequestDTO lessonDto = new LessonRequestDTO(
                LocalDate.of(2026, 3, 10),
                "Ligações Químicas",
                "Aula prática no laboratório",
                tcs.getId(),
                2
        );

        MvcResult lessonRes = mockMvc.perform(post("/api/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Ligações Químicas"))
                .andExpect(jsonPath("$.lessonCount").value(2))
                .andReturn();

        int lessonId = objectMapper.readTree(lessonRes.getResponse().getContentAsString()).get("id").asInt();

        // 2. Registrar falta para o aluno matriculado
        AttendanceRequestDTO attDto = new AttendanceRequestDTO(
                enrolledStudent.getId(),
                lessonId,
                "ABSENT"
        );

        MvcResult attRes = mockMvc.perform(post("/api/attendances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABSENT"))
                .andReturn();

        int attendanceId = objectMapper.readTree(attRes.getResponse().getContentAsString()).get("id").asInt();

        // Frequência deve ser 0.00%
        mockMvc.perform(get("/api/attendances/frequency/student/{studentId}/tcs/{tcsId}", enrolledStudent.getId(), tcs.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("0.00"));

        // 3. Atualizar falta para JUSTIFIED -> JUSTIFIED_ABSENCE conta como presença válida (100.00%)
        AttendanceUpdateRequestDTO updateAttDto = new AttendanceUpdateRequestDTO("JUSTIFIED");
        mockMvc.perform(put("/api/attendances/{id}", attendanceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("JUSTIFIED_ABSENCE"));

        mockMvc.perform(get("/api/attendances/frequency/student/{studentId}/tcs/{tcsId}", enrolledStudent.getId(), tcs.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("100.00"));

        // 4. Criar avaliação no período aberto
        AssessmentRequestDTO assessmentDto = new AssessmentRequestDTO(
                "Prova 1 Química",
                "Avaliação escrita",
                LocalDate.of(2026, 3, 20),
                AssessmentType.EXAM,
                tcs.getId(),
                openPeriod.getId()
        );

        MvcResult assessRes = mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assessmentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Prova 1 Química"))
                .andReturn();

        int assessmentId = objectMapper.readTree(assessRes.getResponse().getContentAsString()).get("id").asInt();

        // 5. Lançar nota para o aluno matriculado
        GradeLaunchRequestDTO gradeDto = new GradeLaunchRequestDTO(
                enrolledStudent.getId(),
                assessmentId,
                new BigDecimal("8.50")
        );

        mockMvc.perform(post("/api/grades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gradeDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(8.5))
                .andExpect(jsonPath("$.studentSituation").value("PENDING"));

        // 6. Consultar média do aluno na disciplina
        mockMvc.perform(get("/api/grades/average/student/{studentId}/tcs/{tcsId}", enrolledStudent.getId(), tcs.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("8.50"));
    }

    @Test
    @WithMockUser(username = "prof.other.ops@example.com", roles = "TEACHER")
    @DisplayName("Professor que não é titular da atribuição recebe 403 Forbidden ao tentar criar aula ou avaliação na turma de outro professor")
    void otherTeacher_cannotCreateLessonOrAssessmentOnUnownedTcs() throws Exception {
        LessonRequestDTO lessonDto = new LessonRequestDTO(
                LocalDate.of(2026, 3, 10),
                "Aula Invadida",
                null,
                tcs.getId(),
                1
        );

        mockMvc.perform(post("/api/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lessonDto)))
                .andExpect(status().isForbidden());

        AssessmentRequestDTO assessmentDto = new AssessmentRequestDTO(
                "Prova Invadida",
                null,
                LocalDate.of(2026, 3, 20),
                AssessmentType.EXAM,
                tcs.getId(),
                openPeriod.getId()
        );

        mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assessmentDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "aluno.enrolled.ops@example.com", roles = "STUDENT")
    @DisplayName("Estudante pode consultar seu próprio boletim e frequência, mas recebe 403 Forbidden ao tentar consultar dados de outro estudante")
    void studentOwnershipEnforcement_allowsOwnAndForbidsOtherStudent() throws Exception {
        // Próprio boletim -> 200 OK
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}",
                        enrolledStudent.getId(), activeYear.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(enrolledStudent.getId()))
                .andExpect(jsonPath("$.classRoomName").value("Turma Ops 1A"));

        // Boletim de outro aluno -> 403 Forbidden
        mockMvc.perform(get("/api/report-cards/student/{studentId}/year/{yearId}",
                        otherStudent.getId(), activeYear.getId()))
                .andExpect(status().isForbidden());

        // Notas de outro aluno -> 403 Forbidden
        mockMvc.perform(get("/api/grades/student/{studentId}", otherStudent.getId()))
                .andExpect(status().isForbidden());

        // Relatório de frequência de outro aluno -> 403 Forbidden
        mockMvc.perform(get("/api/attendances/report/student/{studentId}", otherStudent.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("GET e PUT /api/school-settings atualizam dados cadastrais mantendo critérios sensíveis protegidos")
    void schoolSettings_getAndUpdateByPrincipal() throws Exception {
        MvcResult getRes = mockMvc.perform(get("/api/school-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").isNotEmpty())
                .andReturn();

        JsonNode initial = objectMapper.readTree(getRes.getResponse().getContentAsString());
        BigDecimal initialMinGrade = initial.get("minimumGrade").decimalValue();

        SchoolSettingsRequestDTO updateDto = new SchoolSettingsRequestDTO(
                "Colégio Estadual EscolaWeb Atualizado",
                "Rua das Palmeiras, 500",
                "(48) 3333-9999",
                "contato@escolaweb.edu.br",
                new BigDecimal("8.0"),
                new BigDecimal("85.0")
        );

        mockMvc.perform(put("/api/school-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolName").value("Colégio Estadual EscolaWeb Atualizado"))
                .andExpect(jsonPath("$.address").value("Rua das Palmeiras, 500"))
                .andExpect(jsonPath("$.minimumGrade").value(initialMinGrade.doubleValue()));
    }
}
