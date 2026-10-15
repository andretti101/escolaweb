package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.*;
import com.andretti101.escolaweb.repository.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EnrollmentIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AcademicYear pastYear;
    private AcademicYear activeYear;
    private AcademicPeriod pastPeriod;
    private AcademicPeriod activePeriod;
    private Student student;
    private Teacher teacher;
    private Subject subject;

    @BeforeEach
    void setUp() {
        pastYear = academicYearRepository.findByYear(2024).orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder()
                        .year(2024)
                        .startDate(LocalDate.of(2024, 2, 1))
                        .endDate(LocalDate.of(2024, 12, 15))
                        .active(false)
                        .build()));

        activeYear = academicYearRepository.findByActiveTrue().orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 15))
                        .active(true)
                        .build()));

        pastPeriod = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(pastYear, AcademicPeriodType.TRIMESTER)
                .stream().findFirst().orElseGet(() ->
                        academicPeriodRepository.save(AcademicPeriod.builder()
                                .name("1º Trimestre")
                                .periodType(AcademicPeriodType.TRIMESTER)
                                .startDate(LocalDate.of(2024, 2, 1))
                                .endDate(LocalDate.of(2024, 5, 10))
                                .closed(true)
                                .academicYear(pastYear)
                                .build()));

        activePeriod = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(activeYear, AcademicPeriodType.TRIMESTER)
                .stream().findFirst().orElseGet(() ->
                        academicPeriodRepository.save(AcademicPeriod.builder()
                                .name("1º Trimestre")
                                .periodType(AcademicPeriodType.TRIMESTER)
                                .startDate(LocalDate.of(2026, 2, 1))
                                .endDate(LocalDate.of(2026, 5, 10))
                                .closed(false)
                                .academicYear(activeYear)
                                .build()));

        student = studentRepository.save(Student.builder()
                .name("Aluno Teste Matricula")
                .email("aluno.matricula.teste@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .registrationNumber("20269999")
                .birthDate(LocalDate.of(2010, 5, 15))
                .guardianPhone("(48) 99999-0000")
                .active(true)
                .build());

        teacher = teacherRepository.findAll().stream().findFirst().orElseGet(() ->
                teacherRepository.save(Teacher.builder()
                        .name("Prof Teste Matricula")
                        .email("prof.matricula.teste@example.com")
                        .password(passwordEncoder.encode("senha123"))
                        .role(UserRole.TEACHER)
                        .active(true)
                        .build()));

        subject = subjectRepository.findAll().stream().findFirst().orElseGet(() ->
                subjectRepository.save(Subject.builder()
                        .name("Matéria Teste Matricula")
                        .active(true)
                        .build()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("POST /api/classrooms exige schoolGrade obrigatório e POST /api/enrollments retorna a série da turma")
    void createClassRoomRequiresSchoolGrade_andEnrollmentReturnsSchoolGrade() throws Exception {
        String invalidClassRoomPayload = """
                {
                    "academicYearId": %d,
                    "name": "Turma Sem Serie",
                    "shift": "MORNING",
                    "periodType": "TRIMESTER",
                    "schoolGrade": null
                }
                """.formatted(activeYear.getId());

        mockMvc.perform(post("/api/classrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidClassRoomPayload))
                .andExpect(status().isBadRequest());

        ClassRoom classRoomWithGrade = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Com Serie")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());

        String payload = objectMapper.writeValueAsString(Map.of(
                "studentId", student.getId(),
                "classRoomId", classRoomWithGrade.getId(),
                "enrollmentDate", LocalDate.now().toString()
        ));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.classRoomId").value(classRoomWithGrade.getId()))
                .andExpect(jsonPath("$.classRoomName").value("Turma Com Serie"))
                .andExpect(jsonPath("$.academicYearId").value(activeYear.getId()))
                .andExpect(jsonPath("$.schoolGrade").value(SchoolGrade.MEDIO_1.getLabel()));

        mockMvc.perform(get("/api/enrollments/classroom/{id}", classRoomWithGrade.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$[0].academicYearId").value(activeYear.getId()))
                .andExpect(jsonPath("$[0].schoolGrade").value(SchoolGrade.MEDIO_1.getLabel()));

        mockMvc.perform(get("/api/enrollments/student/{studentId}", student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$[0].academicYearId").value(activeYear.getId()))
                .andExpect(jsonPath("$[0].schoolGrade").value(SchoolGrade.MEDIO_1.getLabel()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("DELETE /api/enrollments/{id} permite excluir matrícula em nova turma mesmo que o aluno possua notas e presenças em turmas anteriores")
    void deleteEnrollment_whenStudentHasGradesAndAttendancesInOtherClassroom_succeeds() throws Exception {
        // Turma de ano anterior onde o aluno teve notas e presenças
        ClassRoom oldClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Antiga 2024")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_8)
                .academicYear(pastYear)
                .active(false)
                .build());

        Enrollment oldEnrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .classRoom(oldClassRoom)
                .enrollmentDate(LocalDate.of(2024, 2, 5))
                .active(false)
                .generalSituation(StudentSituation.APPROVED)
                .build());

        TeacherClassSubject oldTcs = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(teacher)
                .classRoom(oldClassRoom)
                .subject(subject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(false)
                .build());

        Assessment oldAssessment = assessmentRepository.save(Assessment.builder()
                .teacherClassSubject(oldTcs)
                .period(pastPeriod)
                .title("Prova 2024")
                .date(LocalDate.of(2024, 3, 15))
                .assessmentType(AssessmentType.EXAM)
                .active(false)
                .build());

        gradeRepository.save(Grade.builder()
                .student(student)
                .assessment(oldAssessment)
                .value(new BigDecimal("8.50"))
                .active(false)
                .build());

        Lesson oldLesson = lessonRepository.save(Lesson.builder()
                .teacherClassSubject(oldTcs)
                .lessonDate(LocalDate.of(2024, 3, 10))
                .content("Conteúdo 2024")
                .lessonCount(LessonCount.ONE)
                .active(false)
                .build());

        attendanceRepository.save(Attendance.builder()
                .student(student)
                .lesson(oldLesson)
                .status(AttendanceStatus.PRESENT)
                .active(false)
                .build());

        // Nova turma no ano ativo onde o aluno acabou de ser matriculado (sem notas/presenças nesta turma)
        ClassRoom newClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Nova 2026")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());

        Enrollment newEnrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .classRoom(newClassRoom)
                .enrollmentDate(LocalDate.now())
                .active(true)
                .build());

        mockMvc.perform(delete("/api/enrollments/{id}", newEnrollment.getId()))
                .andExpect(status().isNoContent());

        assertFalse(enrollmentRepository.existsById(newEnrollment.getId()));
        assertTrue(enrollmentRepository.existsById(oldEnrollment.getId()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("DELETE /api/enrollments/{id} bloqueia exclusão quando aluno possui notas na própria turma da matrícula")
    void deleteEnrollment_whenStudentHasGradesInSameClassroom_returnsUnprocessableEntity() throws Exception {
        ClassRoom currentClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Com Nota")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());

        Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .classRoom(currentClassRoom)
                .enrollmentDate(LocalDate.now())
                .active(true)
                .build());

        TeacherClassSubject tcs = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(teacher)
                .classRoom(currentClassRoom)
                .subject(subject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        Assessment assessment = assessmentRepository.save(Assessment.builder()
                .teacherClassSubject(tcs)
                .period(activePeriod)
                .title("Prova Atual")
                .date(LocalDate.now())
                .assessmentType(AssessmentType.EXAM)
                .active(true)
                .build());

        gradeRepository.save(Grade.builder()
                .student(student)
                .assessment(assessment)
                .value(new BigDecimal("7.00"))
                .active(true)
                .build());

        mockMvc.perform(delete("/api/enrollments/{id}", enrollment.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("possui notas registradas")));

        assertTrue(enrollmentRepository.existsById(enrollment.getId()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("DELETE /api/enrollments/{id} bloqueia exclusão quando aluno possui presenças na própria turma da matrícula")
    void deleteEnrollment_whenStudentHasAttendancesInSameClassroom_returnsUnprocessableEntity() throws Exception {
        ClassRoom currentClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Com Presenca")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());

        Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .classRoom(currentClassRoom)
                .enrollmentDate(LocalDate.now())
                .active(true)
                .build());

        TeacherClassSubject tcs = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(teacher)
                .classRoom(currentClassRoom)
                .subject(subject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .teacherClassSubject(tcs)
                .lessonDate(LocalDate.now())
                .content("Aula Atual")
                .lessonCount(LessonCount.ONE)
                .active(true)
                .build());

        attendanceRepository.save(Attendance.builder()
                .student(student)
                .lesson(lesson)
                .status(AttendanceStatus.PRESENT)
                .active(true)
                .build());

        mockMvc.perform(delete("/api/enrollments/{id}", enrollment.getId()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("possui presenças registradas")));

        assertTrue(enrollmentRepository.existsById(enrollment.getId()));
    }
}
