package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.TimetableSlotRequestDTO;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TimetableControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private TimetableSlotRepository timetableSlotRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ClassRoom testClassRoom1;
    private ClassRoom testClassRoom2;
    private Teacher testTeacher1;
    private Teacher testTeacher2;
    private Subject testSubjectMath;
    private Subject testSubjectHistory;
    private TeacherClassSubject tcsClass1Math;
    private TeacherClassSubject tcsClass1History;
    private TeacherClassSubject tcsClass2Math;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        AcademicYear year = academicYearRepository.findByActiveTrue()
                .orElseGet(() -> academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .active(true)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 15))
                        .periodType(AcademicPeriodType.TRIMESTER)
                        .build()));

        testClassRoom1 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma T-101")
                .schoolGrade(SchoolGrade.MEDIO_1)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .active(true)
                .build());

        testClassRoom2 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma T-102")
                .schoolGrade(SchoolGrade.MEDIO_1)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .active(true)
                .build());

        testTeacher1 = teacherRepository.save(Teacher.builder()
                .name("Prof. Teste Alpha")
                .email("alpha.teacher.test@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .active(true)
                .build());

        testTeacher2 = teacherRepository.save(Teacher.builder()
                .name("Profa. Teste Beta")
                .email("beta.teacher.test@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .active(true)
                .build());

        testSubjectMath = subjectRepository.save(Subject.builder()
                .name("Matemática Avançada Teste")
                .active(true)
                .build());

        testSubjectHistory = subjectRepository.save(Subject.builder()
                .name("História Contemporânea Teste")
                .active(true)
                .build());

        tcsClass1Math = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .classRoom(testClassRoom1)
                .teacher(testTeacher1)
                .subject(testSubjectMath)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(5)
                .active(true)
                .build());

        tcsClass1History = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .classRoom(testClassRoom1)
                .teacher(testTeacher2)
                .subject(testSubjectHistory)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(5)
                .active(true)
                .build());

        tcsClass2Math = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .classRoom(testClassRoom2)
                .teacher(testTeacher1) // mesmo professor Alpha
                .subject(testSubjectMath)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(5)
                .active(true)
                .build());

        testStudent = studentRepository.save(Student.builder()
                .name("Aluno Teste Timetable")
                .email("student.timetable.test@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .registrationNumber("MAT-TEST-9999")
                .active(true)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(testStudent)
                .classRoom(testClassRoom1)
                .enrollmentDate(LocalDate.now())
                .active(true)
                .build());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Direção: pode cadastrar período na grade com sucesso (201 Created)")
    void principalCanCreateSlot() throws Exception {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(7, 30),
                LocalTime.of(8, 20),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.classRoomId").value(testClassRoom1.getId()))
                .andExpect(jsonPath("$.subjectName").value("Matemática Avançada Teste"))
                .andExpect(jsonPath("$.teacherName").value("Prof. Teste Alpha"))
                .andExpect(jsonPath("$.dayOfWeek").value("SEGUNDA"))
                .andExpect(jsonPath("$.startTime").value("07:30"))
                .andExpect(jsonPath("$.endTime").value("08:20"))
                .andExpect(jsonPath("$.slotOrder").value(1));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Direção: pode atualizar e remover horário na grade")
    void principalCanUpdateAndDeleteSlot() throws Exception {
        TimetableSlot slot = timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.TERCA)
                .startTime(LocalTime.of(9, 10))
                .endTime(LocalTime.of(10, 0))
                .slotOrder(3)
                .build());

        TimetableSlotRequestDTO updateDto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.TERCA,
                LocalTime.of(9, 20),
                LocalTime.of(10, 10),
                3
        );

        mockMvc.perform(put("/api/timetable/" + slot.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startTime").value("09:20"))
                .andExpect(jsonPath("$.endTime").value("10:10"));

        mockMvc.perform(delete("/api/timetable/" + slot.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/timetable/" + slot.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    @DisplayName("Secretaria: NÃO PODE criar horário na grade (403 Forbidden)")
    void secretaryCannotCreateSlot() throws Exception {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(7, 30),
                LocalTime.of(8, 20),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    @DisplayName("Secretaria: NÃO PODE deletar horário na grade (403 Forbidden)")
    void secretaryCannotDeleteSlot() throws Exception {
        TimetableSlot slot = timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        mockMvc.perform(delete("/api/timetable/" + slot.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alpha.teacher.test@example.com", roles = "TEACHER")
    @DisplayName("Professor: NÃO PODE criar horário na grade (403 Forbidden)")
    void teacherCannotCreateSlot() throws Exception {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(7, 30),
                LocalTime.of(8, 20),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student.timetable.test@example.com", roles = "STUDENT")
    @DisplayName("Aluno: NÃO PODE criar horário na grade (403 Forbidden)")
    void studentCannotCreateSlot() throws Exception {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(7, 30),
                LocalTime.of(8, 20),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    @DisplayName("Secretaria: PODE consultar grade por turma e por professor (200 OK)")
    void secretaryCanReadClassroomAndTeacherSchedule() throws Exception {
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.QUARTA)
                .startTime(LocalTime.of(8, 20))
                .endTime(LocalTime.of(9, 10))
                .slotOrder(2)
                .build());

        mockMvc.perform(get("/api/timetable/classroom/" + testClassRoom1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classRoomId").value(testClassRoom1.getId()))
                .andExpect(jsonPath("$.slots", hasSize(1)));

        mockMvc.perform(get("/api/timetable/teacher/" + testTeacher1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(testTeacher1.getId()))
                .andExpect(jsonPath("$.slots", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "student.timetable.test@example.com", roles = "STUDENT")
    @DisplayName("Aluno: PODE consultar a própria grade via /my-schedule (200 OK)")
    void studentCanReadMySchedule() throws Exception {
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.QUINTA)
                .startTime(LocalTime.of(10, 20))
                .endTime(LocalTime.of(11, 10))
                .slotOrder(4)
                .build());

        mockMvc.perform(get("/api/timetable/my-schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classRoomId").value(testClassRoom1.getId()))
                .andExpect(jsonPath("$.slots", hasSize(1)))
                .andExpect(jsonPath("$.slots[0].dayOfWeek").value("QUINTA"));
    }

    @Test
    @WithMockUser(username = "alpha.teacher.test@example.com", roles = "TEACHER")
    @DisplayName("Professor: PODE consultar seus próprios horários via /my-schedule (200 OK)")
    void teacherCanReadMySchedule() throws Exception {
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.SEXTA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        mockMvc.perform(get("/api/timetable/my-schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(testTeacher1.getId()))
                .andExpect(jsonPath("$.slots", hasSize(1)))
                .andExpect(jsonPath("$.slots[0].dayOfWeek").value("SEXTA"));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Choque de horário do professor: rejeita com 409 Conflict")
    void conflictWhenTeacherCollision() throws Exception {
        // Horário existente: Carlos na Turma 1 na Segunda 07:30-08:20
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        // Tentativa de agendar Carlos na Turma 2 na mesma Segunda sobrepondo (08:00 - 08:50)
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom2.getId(),
                tcsClass2Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(8, 0),
                LocalTime.of(8, 50),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Choque de horário do professor")));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Choque de horário da turma: rejeita com 409 Conflict")
    void conflictWhenClassRoomCollision() throws Exception {
        // Horário existente: Turma 1 tem Matemática na Segunda 07:30-08:20
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        // Tentativa de agendar História na Turma 1 na mesma Segunda sobrepondo (07:30 - 08:20)
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1History.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(7, 30),
                LocalTime.of(8, 20),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Choque de horário na turma")));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Validação: horário de início posterior ao término rejeita com 400 Bad Request")
    void invalidTimeRejection() throws Exception {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(8, 20),
                LocalTime.of(7, 30),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Visualização Direção: rota /direcao/grade-horaria renderiza principal/timetable")
    void principalTimetableTemplate() throws Exception {
        mockMvc.perform(get("/direcao/grade-horaria"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/timetable"));
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    @DisplayName("Visualização Secretaria: rota /secretaria/grade-horaria renderiza secretary/timetable")
    void secretaryTimetableTemplate() throws Exception {
        mockMvc.perform(get("/secretaria/grade-horaria"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary/timetable"));
    }

    @Test
    @WithMockUser(username = "alpha.teacher.test@example.com", roles = "TEACHER")
    @DisplayName("Visualização Professor: rota /professor/grade-horaria renderiza teacher/timetable")
    void teacherTimetableTemplate() throws Exception {
        mockMvc.perform(get("/professor/grade-horaria"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/timetable"));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Visualização Aluno: rota /aluno/grade-horaria renderiza student/timetable")
    void studentTimetableTemplate() throws Exception {
        mockMvc.perform(get("/aluno/grade-horaria"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/timetable"));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Professor em anos letivos distintos: NÃO gera falso choque de horário (201 Created)")
    void noConflictWhenTeacherInDifferentAcademicYear() throws Exception {
        // Obter ou criar ano letivo anterior (2025)
        AcademicYear pastYear = academicYearRepository.findByYear(2025)
                .orElseGet(() -> academicYearRepository.save(AcademicYear.builder()
                        .year(2025)
                        .active(false)
                        .startDate(LocalDate.of(2025, 2, 1))
                        .endDate(LocalDate.of(2025, 12, 15))
                        .periodType(AcademicPeriodType.TRIMESTER)
                        .build()));

        ClassRoom pastClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma 2025 Antiga")
                .schoolGrade(SchoolGrade.MEDIO_1)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(pastYear)
                .active(true)
                .build());

        TeacherClassSubject pastTcs = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .classRoom(pastClassRoom)
                .teacher(testTeacher1)
                .subject(testSubjectMath)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(5)
                .active(true)
                .build());

        // Horário de aula no ano de 2025: Segunda 07:30 às 08:20
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(pastClassRoom)
                .teacherClassSubject(pastTcs)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        // Agora Direção agenda o mesmo professor na Turma 2026 no mesmo horário (Segunda 07:30 às 08:20)
        TimetableSlotRequestDTO dto2026 = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(7, 30),
                LocalTime.of(8, 20),
                1
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2026)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.classRoomId").value(testClassRoom1.getId()));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Horários adjacentes na fronteira (ex: 07:30-08:20 e 08:20-09:10) não colidem")
    void adjacentSlotsDoNotCollide() throws Exception {
        // Aula 1: 07:30 - 08:20
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        // Aula 2: 08:20 - 09:10 (fronteira exata em 08:20)
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1History.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(8, 20),
                LocalTime.of(9, 10),
                2
        );

        mockMvc.perform(post("/api/timetable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("08:20"))
                .andExpect(jsonPath("$.endTime").value("09:10"));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Atualização com colisão de horário rejeita com 409 Conflict")
    void updateSlotConflictRejected() throws Exception {
        TimetableSlot slot1 = timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1History)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(8, 20))
                .endTime(LocalTime.of(9, 10))
                .slotOrder(2)
                .build());

        // Tenta atualizar slot1 para 08:00 - 08:40 (colidindo com o slot2 de História)
        TimetableSlotRequestDTO updateDto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                DayOfWeekEnum.SEGUNDA,
                LocalTime.of(8, 0),
                LocalTime.of(8, 40),
                1
        );

        mockMvc.perform(put("/api/timetable/" + slot1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Choque de horário na turma")));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Grade da turma é retornada em ordem cronológica de dias da semana (Segunda antes de Quarta)")
    void slotsAreOrderedChronologicallyByDayOfWeek() throws Exception {
        // Inserir Quarta primeiro no banco
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.QUARTA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        // Inserir Segunda depois no banco
        timetableSlotRepository.save(TimetableSlot.builder()
                .classRoom(testClassRoom1)
                .teacherClassSubject(tcsClass1Math)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build());

        mockMvc.perform(get("/api/timetable/classroom/" + testClassRoom1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(2)))
                .andExpect(jsonPath("$.slots[0].dayOfWeek").value("SEGUNDA"))
                .andExpect(jsonPath("$.slots[1].dayOfWeek").value("QUARTA"));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Geração automática da grade cria horários sem colisão de professores e suporta 6ª aula para turmas matutino/vespertino")
    void autoGenerate_createsSlotsWithoutCollisionsAndSupportsSixthLesson() throws Exception {
        // testTeacher1 ensina Matemática na turma 1 (3 aulas) e na turma 2 (3 aulas)
        tcsClass1Math.setWeeklyLessons(3);
        teacherClassSubjectRepository.save(tcsClass1Math);

        tcsClass1History.setWeeklyLessons(2);
        teacherClassSubjectRepository.save(tcsClass1History);

        tcsClass2Math.setWeeklyLessons(3);
        teacherClassSubjectRepository.save(tcsClass2Math);

        // Criar turma 3 no Vespertino com 6ª aula
        ClassRoom testClassRoom3 = classRoomRepository.save(ClassRoom.builder()
                .name("Turma T-Vespertino")
                .schoolGrade(SchoolGrade.MEDIO_2)
                .shift(Shift.AFTERNOON)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(testClassRoom1.getAcademicYear())
                .active(true)
                .build());

        TeacherClassSubject tcsClass3Math = teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .classRoom(testClassRoom3)
                .teacher(testTeacher1)
                .subject(testSubjectMath)
                .weeklyLessons(4)
                .active(true)
                .build());

        Map<String, Object> req = Map.of(
                "lessonDurationMinutes", 50,
                "startTime", "07:00",
                "afternoonStartTime", "13:00",
                "classroomsWithSixthLessonIds", java.util.List.of(testClassRoom3.getId()),
                "classRoomIds", java.util.List.of(testClassRoom1.getId(), testClassRoom2.getId(), testClassRoom3.getId())
        );

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomsCount").value(3))
                .andExpect(jsonPath("$.totalSlotsGenerated").value(3 + 2 + 3 + 4)) // 12 aulas no total
                .andExpect(jsonPath("$.message", containsString("Grade horária gerada com sucesso")));

        // Verificar turma 1 (5 aulas no total)
        mockMvc.perform(get("/api/timetable/classroom/" + testClassRoom1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(5)));

        // Verificar turma 2 (3 aulas no total)
        mockMvc.perform(get("/api/timetable/classroom/" + testClassRoom2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(3)));

        // Verificar turma 3 (4 aulas no total)
        mockMvc.perform(get("/api/timetable/classroom/" + testClassRoom3.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots", hasSize(4)));

        // Verificar que não há choque de horário para o testTeacher1 entre turma 1 e turma 2
        var teacherSlots = timetableSlotRepository.findByTeacherAndAcademicYear(testTeacher1.getId(), null);
        for (int i = 0; i < teacherSlots.size(); i++) {
            for (int j = i + 1; j < teacherSlots.size(); j++) {
                var s1 = teacherSlots.get(i);
                var s2 = teacherSlots.get(j);
                if (s1.getDayOfWeek() == s2.getDayOfWeek()) {
                    boolean overlap = s1.getStartTime().isBefore(s2.getEndTime()) && s2.getStartTime().isBefore(s1.getEndTime());
                    org.junit.jupiter.api.Assertions.assertFalse(overlap, "Choque detectado entre horários do professor!");
                }
            }
        }
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Geração automática rejeita 6ª aula para turmas do turno Noturno ou Integral")
    void autoGenerate_rejectsSixthLessonForNightClass() throws Exception {
        ClassRoom nightClass = classRoomRepository.save(ClassRoom.builder()
                .name("Turma Noturna 101")
                .schoolGrade(SchoolGrade.MEDIO_1)
                .shift(Shift.EVENING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(testClassRoom1.getAcademicYear())
                .active(true)
                .build());

        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .classRoom(nightClass)
                .teacher(testTeacher1)
                .subject(testSubjectMath)
                .weeklyLessons(2)
                .active(true)
                .build());

        Map<String, Object> req = Map.of(
                "lessonDurationMinutes", 45,
                "startTime", "07:30",
                "classroomsWithSixthLessonIds", java.util.List.of(nightClass.getId()),
                "classRoomIds", java.util.List.of(nightClass.getId())
        );

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Apenas turmas dos turnos Matutino e Vespertino podem possuir a 6ª aula")));
    }

    @Test
    @WithMockUser(username = "secretaria@example.com", roles = "SECRETARY")
    @DisplayName("Secretaria não tem permissão para disparar geração automática da grade horária")
    void autoGenerate_forbiddenForSecretary() throws Exception {
        Map<String, Object> req = Map.of(
                "lessonDurationMinutes", 50,
                "startTime", "07:00"
        );
        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Permite editar e excluir horários gerados automaticamente")
    void autoGenerate_allowsEditingAndDeletingGeneratedSlots() throws Exception {
        tcsClass1Math.setWeeklyLessons(1);
        teacherClassSubjectRepository.save(tcsClass1Math);

        Map<String, Object> req = Map.of(
                "lessonDurationMinutes", 50,
                "startTime", "07:30",
                "classRoomIds", java.util.List.of(testClassRoom1.getId())
        );

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        var slots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(testClassRoom1.getId());
        org.junit.jupiter.api.Assertions.assertFalse(slots.isEmpty());
        TimetableSlot generatedSlot = slots.get(0);

        java.util.Set<DayOfWeekEnum> usedDays = slots.stream()
                .map(TimetableSlot::getDayOfWeek)
                .collect(java.util.stream.Collectors.toSet());
        DayOfWeekEnum freeDay = java.util.Arrays.stream(DayOfWeekEnum.values())
                .filter(d -> d != DayOfWeekEnum.SABADO && !usedDays.contains(d))
                .findFirst()
                .orElse(DayOfWeekEnum.SABADO);

        // Editar horário gerado para um dia livre da turma
        TimetableSlotRequestDTO updateDto = new TimetableSlotRequestDTO(
                testClassRoom1.getId(),
                tcsClass1Math.getId(),
                freeDay,
                LocalTime.of(10, 0),
                LocalTime.of(10, 50),
                3
        );

        mockMvc.perform(put("/api/timetable/" + generatedSlot.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value(freeDay.name()))
                .andExpect(jsonPath("$.slotOrder").value(3));

        // Excluir horário gerado
        mockMvc.perform(delete("/api/timetable/" + generatedSlot.getId()))
                .andExpect(status().isNoContent());

        var remaining = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(testClassRoom1.getId());
        org.junit.jupiter.api.Assertions.assertEquals(slots.size() - 1, remaining.size());
        org.junit.jupiter.api.Assertions.assertTrue(remaining.stream().noneMatch(s -> s.getId().equals(generatedSlot.getId())));
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Geração sequencial para turmas distintas com professor compartilhado evita choque de horários")
    void autoGenerate_sequentialGenerationForDifferentClassroomsAvoidsTeacherConflicts() throws Exception {
        tcsClass1Math.setWeeklyLessons(2);
        teacherClassSubjectRepository.save(tcsClass1Math);
        tcsClass2Math.setWeeklyLessons(2);
        teacherClassSubjectRepository.save(tcsClass2Math);

        // 1. Gera grade apenas para Turma 1
        Map<String, Object> req1 = Map.of(
                "lessonDurationMinutes", 50,
                "startTime", "07:30",
                "classRoomIds", java.util.List.of(testClassRoom1.getId())
        );

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // 2. Gera grade apenas para Turma 2
        Map<String, Object> req2 = Map.of(
                "lessonDurationMinutes", 50,
                "startTime", "07:30",
                "classRoomIds", java.util.List.of(testClassRoom2.getId())
        );

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        // 3. Verifica que testTeacher1 (Alpha) não possui choque entre Turma 1 e Turma 2
        var teacherSlots = timetableSlotRepository.findByTeacherAndAcademicYear(testTeacher1.getId(), null);
        org.junit.jupiter.api.Assertions.assertFalse(teacherSlots.isEmpty());
        for (int i = 0; i < teacherSlots.size(); i++) {
            for (int j = i + 1; j < teacherSlots.size(); j++) {
                var s1 = teacherSlots.get(i);
                var s2 = teacherSlots.get(j);
                if (s1.getDayOfWeek() == s2.getDayOfWeek()) {
                    boolean overlap = s1.getStartTime().isBefore(s2.getEndTime()) && s2.getStartTime().isBefore(s1.getEndTime());
                    org.junit.jupiter.api.Assertions.assertFalse(overlap,
                            String.format("Choque detectado para o professor %s: %s %s-%s e %s-%s",
                                    testTeacher1.getName(), s1.getDayOfWeek(), s1.getStartTime(), s1.getEndTime(), s2.getStartTime(), s2.getEndTime()));
                }
            }
        }
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Geração automática respeita 45 min, início 07:50 (término 11:45 com 10 min de intervalo), tarde 13:15, 6ª aula (30 aulas/sem) e Integral (10 aulas/dia = 50/sem)")
    void autoGenerate_respects45MinTimesBreakSixthLessonAndIntegral10LessonsPerDay() throws Exception {
        AcademicYear year = testClassRoom1.getAcademicYear();

        // Criar 10 professores dedicados para o teste de carga horária completa
        java.util.List<Teacher> teachers = new java.util.ArrayList<>();
        java.util.List<Subject> subjects = new java.util.ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            Teacher t = teacherRepository.save(Teacher.builder()
                    .name("Prof. Grade Completa " + i)
                    .email("prof.grade.completa." + i + "@example.com")
                    .password(passwordEncoder.encode("senha123"))
                    .role(UserRole.TEACHER)
                    .active(true)
                    .build());
            teachers.add(t);

            Subject s = subjectRepository.save(Subject.builder()
                    .name("Disciplina Grade " + i)
                    .active(true)
                    .build());
            subjects.add(s);
        }

        // 1. Turma Matutino Regular (5 aulas/dia = 25/semana)
        ClassRoom morningRegular = classRoomRepository.save(ClassRoom.builder()
                .name("T-Mat 5 Aulas")
                .schoolGrade(SchoolGrade.FUNDAMENTAL_6)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .active(true)
                .build());

        // 2. Turma Matutino com 6ª Aula (cadastrada com 25 aulas nas matérias, mas marcada com 6ª aula -> deve expandir para 30 aulas/semana = 6/dia)
        ClassRoom morningSixth = classRoomRepository.save(ClassRoom.builder()
                .name("T-Mat 6 Aulas")
                .schoolGrade(SchoolGrade.FUNDAMENTAL_7)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .active(true)
                .build());

        // 3. Turma Integral (10 aulas/dia = 50/semana: 5 manhã + 5 tarde)
        ClassRoom fullTimeClass = classRoomRepository.save(ClassRoom.builder()
                .name("T-Integral 10")
                .schoolGrade(SchoolGrade.MEDIO_1)
                .shift(Shift.FULL_TIME)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .active(true)
                .build());

        int[] regularLoads = {4, 4, 4, 3, 3, 3, 2, 2}; // soma = 25
        for (int i = 0; i < 8; i++) {
            teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                    .classRoom(morningRegular)
                    .teacher(teachers.get(i))
                    .subject(subjects.get(i))
                    .weeklyLessons(regularLoads[i])
                    .active(true)
                    .build());

            teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                    .classRoom(morningSixth)
                    .teacher(teachers.get((i + 3) % 14))
                    .subject(subjects.get(i))
                    .weeklyLessons(regularLoads[i])
                    .active(true)
                    .build());
        }

        int[] integralLoads = {4, 4, 4, 4, 4, 4, 4, 4, 3, 3, 3, 3, 3, 3}; // soma = 50 (14 disciplinas)
        for (int i = 0; i < 14; i++) {
            teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                    .classRoom(fullTimeClass)
                    .teacher(teachers.get(i))
                    .subject(subjects.get(i))
                    .weeklyLessons(integralLoads[i])
                    .active(true)
                    .build());
        }

        Map<String, Object> req = Map.of(
                "lessonDurationMinutes", 45,
                "startTime", "07:50",
                "classroomsWithSixthLessonIds", java.util.List.of(morningSixth.getId()),
                "classRoomIds", java.util.List.of(morningRegular.getId(), morningSixth.getId(), fullTimeClass.getId())
        );

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomsCount").value(3))
                .andExpect(jsonPath("$.totalSlotsGenerated").value(25 + 30 + 50));

        // Verificar Turma Regular: 25 aulas (5 por dia), 1ª aula 07:50-08:35 e 5ª aula 11:00-11:45 (após intervalo 10:05-10:15)
        var regularSlots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(morningRegular.getId());
        org.junit.jupiter.api.Assertions.assertEquals(25, regularSlots.size());
        var mondayReg = regularSlots.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(5, mondayReg.size());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(7, 50), mondayReg.get(0).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(8, 35), mondayReg.get(0).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(9, 20), mondayReg.get(2).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(10, 5), mondayReg.get(2).getEndTime());
        // 4ª aula começa às 10:15 (após 10 min de intervalo)
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(10, 15), mondayReg.get(3).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 0), mondayReg.get(3).getEndTime());
        // 5ª aula termina exatamente às 11:45
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 0), mondayReg.get(4).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 45), mondayReg.get(4).getEndTime());

        // Verificar Turma com 6ª Aula: 30 aulas (6 por dia), 6ª aula 11:45-12:30
        var sixthSlots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(morningSixth.getId());
        org.junit.jupiter.api.Assertions.assertEquals(30, sixthSlots.size());
        var mondaySixth = sixthSlots.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(6, mondaySixth.size());
        org.junit.jupiter.api.Assertions.assertEquals(6, mondaySixth.get(5).getSlotOrder());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 45), mondaySixth.get(5).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(12, 30), mondaySixth.get(5).getEndTime());

        // Verificar Turma Integral: 50 aulas (10 por dia: 5 manhã 07:50-11:45 + 5 tarde 13:15-17:10)
        var integralSlots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(fullTimeClass.getId());
        org.junit.jupiter.api.Assertions.assertEquals(50, integralSlots.size());
        var mondayInt = integralSlots.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(10, mondayInt.size());
        // Manhã: 1ª (07:50-08:35) até 5ª (11:00-11:45)
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(7, 50), mondayInt.get(0).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 45), mondayInt.get(4).getEndTime());
        // Tarde: 6ª aula (1ª da tarde) inicia às 13:15 e 10ª aula (5ª da tarde) termina às 17:10 (com intervalo 15:30-15:40)
        org.junit.jupiter.api.Assertions.assertEquals(6, mondayInt.get(5).getSlotOrder());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(13, 15), mondayInt.get(5).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(14, 0), mondayInt.get(5).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(15, 40), mondayInt.get(8).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(10, mondayInt.get(9).getSlotOrder());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(16, 25), mondayInt.get(9).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(17, 10), mondayInt.get(9).getEndTime());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Seed data.sql: gera grade completa para as 5 turmas ativas de 2026 em todas as combinações de 6ª aula")
    void autoGenerate_seededClassroomsFromDataSql() throws Exception {
        java.util.List<java.util.List<Integer>> combinations = java.util.List.of(
                java.util.List.of(),
                java.util.List.of(7),
                java.util.List.of(8),
                java.util.List.of(9),
                java.util.List.of(7, 8),
                java.util.List.of(7, 9),
                java.util.List.of(8, 9),
                java.util.List.of(7, 8, 9)
        );

        for (java.util.List<Integer> sixthIds : combinations) {
            Map<String, Object> req = Map.of(
                    "lessonDurationMinutes", 45,
                    "startTime", "07:50",
                    "classroomsWithSixthLessonIds", sixthIds,
                    "classRoomIds", java.util.List.of(7, 8, 9, 14, 15)
            );

            int expected7 = sixthIds.contains(7) ? 30 : 25;
            int expected8 = sixthIds.contains(8) ? 30 : 25;
            int expected9 = sixthIds.contains(9) ? 30 : 25;
            int expectedTotal = expected7 + expected8 + expected9 + 50 + 50;

            mockMvc.perform(post("/api/timetable/auto-generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.classroomsCount").value(5))
                    .andExpect(jsonPath("$.totalSlotsGenerated").value(expectedTotal));

            org.junit.jupiter.api.Assertions.assertEquals(expected7,
                    timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(7).size(),
                    "Falha na contagem da turma 7 para combinação " + sixthIds);
            org.junit.jupiter.api.Assertions.assertEquals(expected8,
                    timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(8).size(),
                    "Falha na contagem da turma 8 para combinação " + sixthIds);
            org.junit.jupiter.api.Assertions.assertEquals(expected9,
                    timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(9).size(),
                    "Falha na contagem da turma 9 para combinação " + sixthIds);
            org.junit.jupiter.api.Assertions.assertEquals(50,
                    timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(14).size(),
                    "Falha na contagem da turma 14 para combinação " + sixthIds);
            org.junit.jupiter.api.Assertions.assertEquals(50,
                    timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(15).size(),
                    "Falha na contagem da turma 15 para combinação " + sixthIds);
        }
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Gera grade para TODAS as 6 turmas ativas de 2026 simultaneamente (incluindo turma noturna ID 18)")
    void autoGenerate_allActiveClassroomsIncludingNight() throws Exception {
        testClassRoom1.setActive(false);
        classRoomRepository.save(testClassRoom1);
        testClassRoom2.setActive(false);
        classRoomRepository.save(testClassRoom2);

        Map<String, Object> req = new java.util.HashMap<>();
        req.put("morningStartTime", "07:50");
        req.put("morningEndTime", "11:45");
        req.put("afternoonStartTime", "13:15");
        req.put("afternoonEndTime", "17:10");
        req.put("nightStartTime", "19:00");
        req.put("nightEndTime", "22:30");
        req.put("breakDurationMinutes", 10);
        req.put("classroomsWithSixthLessonIds", java.util.List.of(7));
        // classRoomIds omitted -> applies to all 6 active classrooms of 2026: 7, 8, 9, 14, 15, 18

        // Turma 7 (M1-A com 6ª aula): 30
        // Turma 8 (M2-A regular): 25
        // Turma 9 (M3-A vespertino): 25
        // Turma 14 (M1-INT integral): 50
        // Turma 15 (M2-INT integral): 50
        // Turma 18 (M3-NOT noturno): 25
        int expectedTotal = 30 + 25 + 25 + 50 + 50 + 25; // 205 aulas

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomsCount").value(6))
                .andExpect(jsonPath("$.totalSlotsGenerated").value(expectedTotal));

        // Verificar turma noturna ID 18
        var nightSlots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(18);
        org.junit.jupiter.api.Assertions.assertEquals(25, nightSlots.size(), "Turma noturna deve possuir exatamente 25 aulas semanais");
        
        var mondayNight = nightSlots.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(5, mondayNight.size(), "Turma noturna possui 5 aulas/dia (sem 6ª aula)");
        
        // 1ª aula: 19:00 - 19:40
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(19, 0), mondayNight.get(0).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(19, 40), mondayNight.get(0).getEndTime());
        // 2ª aula: 19:40 - 20:20
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(19, 40), mondayNight.get(1).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(20, 20), mondayNight.get(1).getEndTime());
        // 3ª aula: 20:20 - 21:00
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(20, 20), mondayNight.get(2).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 0), mondayNight.get(2).getEndTime());
        // Intervalo de 10 minutos (21:00 - 21:10)
        // 4ª aula: 21:10 - 21:50
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 10), mondayNight.get(3).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 50), mondayNight.get(3).getEndTime());
        // 5ª aula: 21:50 - 22:30
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 50), mondayNight.get(4).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(22, 30), mondayNight.get(4).getEndTime());

        // Verificar ausência de conflitos de professores em todos os 205 horários
        var allSlots = timetableSlotRepository.findAll();
        for (int i = 0; i < allSlots.size(); i++) {
            var s1 = allSlots.get(i);
            for (int j = i + 1; j < allSlots.size(); j++) {
                var s2 = allSlots.get(j);
                if (s1.getDayOfWeek() == s2.getDayOfWeek()
                        && s1.getTeacherClassSubject().getTeacher().getId().equals(s2.getTeacherClassSubject().getTeacher().getId())) {
                    boolean overlaps = s1.getStartTime().isBefore(s2.getEndTime()) && s2.getStartTime().isBefore(s1.getEndTime());
                    org.junit.jupiter.api.Assertions.assertFalse(overlaps,
                            "Conflito de professor detectado entre " + s1.getId() + " e " + s2.getId());
                }
            }
        }
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Regra de dedução de minutos na 4ª aula quando a divisão de horários não for exata")
    void autoGenerate_excessDeductedOnFourthLesson() throws Exception {
        // Manhã: 07:50 até 11:43 (233 minutos) com intervalo de 10 min -> tempo líquido = 223 min.
        // 5 aulas -> ceil(223 / 5) = 45 min. Excess = 5*45 - 223 = 2 min.
        // 1ª, 2ª, 3ª aulas: 45 min cada (07:50-08:35, 08:35-09:20, 09:20-10:05).
        // Intervalo: 10 min (10:05-10:15).
        // 4ª aula com dedução dos 2 minutos: 45 - 2 = 43 min (10:15-10:58).
        // 5ª aula: 45 min (10:58-11:43).
        // Noite: 19:00 até 22:28 (208 minutos) com intervalo de 10 min -> tempo líquido = 198 min.
        // 5 aulas -> ceil(198 / 5) = 40 min. Excess = 5*40 - 198 = 2 min.
        // 4ª aula noturna com dedução dos 2 minutos: 40 - 2 = 38 min (21:10-21:48).
        // 5ª aula noturna: 40 min (21:48-22:28).

        Map<String, Object> req = Map.of(
                "morningStartTime", "07:50",
                "morningEndTime", "11:43",
                "afternoonStartTime", "13:15",
                "afternoonEndTime", "17:10",
                "nightStartTime", "19:00",
                "nightEndTime", "22:28",
                "breakDurationMinutes", 10,
                "classroomsWithSixthLessonIds", java.util.List.of(),
                "classRoomIds", java.util.List.of(8, 18)
        );

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomsCount").value(2))
                .andExpect(jsonPath("$.totalSlotsGenerated").value(50));

        // Verificação turma da manhã (ID 8)
        var morningSlots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(8);
        var mondayMorn = morningSlots.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(5, mondayMorn.size());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(7, 50), mondayMorn.get(0).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(8, 35), mondayMorn.get(0).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(9, 20), mondayMorn.get(1).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(10, 5), mondayMorn.get(2).getEndTime());
        // Intervalo 10 min: 10:05 até 10:15
        // 4ª aula: 43 minutos de duração (10:15 - 10:58)
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(10, 15), mondayMorn.get(3).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(10, 58), mondayMorn.get(3).getEndTime());
        // 5ª aula: 45 minutos de duração (10:58 - 11:43)
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(10, 58), mondayMorn.get(4).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 43), mondayMorn.get(4).getEndTime());

        // Verificação turma da noite (ID 18)
        var nightSlots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(18);
        var mondayNight = nightSlots.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(5, mondayNight.size());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(19, 0), mondayNight.get(0).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(19, 40), mondayNight.get(0).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(20, 20), mondayNight.get(1).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 0), mondayNight.get(2).getEndTime());
        // Intervalo 10 min: 21:00 até 21:10
        // 4ª aula: 38 minutos de duração (21:10 - 21:48)
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 10), mondayNight.get(3).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 48), mondayNight.get(3).getEndTime());
        // 5ª aula: 40 minutos de duração (21:48 - 22:28)
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(21, 48), mondayNight.get(4).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(22, 28), mondayNight.get(4).getEndTime());
    }

    @Test
    @WithMockUser(username = "direcao@example.com", roles = "PRINCIPAL")
    @DisplayName("Gera grade com saída da manhã 12:30: turma com 6ª aula termina 12:30 e turma regular 5 aulas termina 11:45 em sincronia perfeita")
    void autoGenerate_morningEndTime1230_synchronizedSlots() throws Exception {
        testClassRoom1.setActive(false);
        classRoomRepository.save(testClassRoom1);
        testClassRoom2.setActive(false);
        classRoomRepository.save(testClassRoom2);

        Map<String, Object> req = new java.util.HashMap<>();
        req.put("morningStartTime", "07:50");
        req.put("morningEndTime", "12:30");
        req.put("afternoonStartTime", "13:15");
        req.put("afternoonEndTime", "17:10");
        req.put("nightStartTime", "19:00");
        req.put("nightEndTime", "22:30");
        req.put("breakDurationMinutes", 10);
        req.put("classroomsWithSixthLessonIds", java.util.List.of(7));

        mockMvc.perform(post("/api/timetable/auto-generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classroomsCount").value(6))
                .andExpect(jsonPath("$.totalSlotsGenerated").value(205));

        // Turma 7 (com 6ª aula): 6 aulas/dia terminando às 12:30
        var slots7 = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(7);
        var monday7 = slots7.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(6, monday7.size());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(7, 50), monday7.get(0).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(8, 35), monday7.get(0).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 45), monday7.get(4).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 45), monday7.get(5).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(12, 30), monday7.get(5).getEndTime());

        // Turma 8 (regular 5 aulas): 5 aulas/dia terminando às 11:45
        var slots8 = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(8);
        var monday8 = slots8.stream().filter(s -> s.getDayOfWeek() == DayOfWeekEnum.SEGUNDA).toList();
        org.junit.jupiter.api.Assertions.assertEquals(5, monday8.size());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(7, 50), monday8.get(0).getStartTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(8, 35), monday8.get(0).getEndTime());
        org.junit.jupiter.api.Assertions.assertEquals(LocalTime.of(11, 45), monday8.get(4).getEndTime());

        // Aulas 1 a 5 de Turma 7 e Turma 8 devem ter exatamente os mesmos horários de início e término
        for (int i = 0; i < 5; i++) {
            org.junit.jupiter.api.Assertions.assertEquals(monday7.get(i).getStartTime(), monday8.get(i).getStartTime());
            org.junit.jupiter.api.Assertions.assertEquals(monday7.get(i).getEndTime(), monday8.get(i).getEndTime());
        }
    }
}
