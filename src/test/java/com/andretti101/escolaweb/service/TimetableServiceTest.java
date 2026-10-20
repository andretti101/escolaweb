package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.TimetableSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.TimetableSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyScheduleDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.exception.ConflictException;
import com.andretti101.escolaweb.service.impl.TimetableServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableSlotRepository timetableSlotRepository;

    @Mock
    private ClassRoomRepository classRoomRepository;

    @Mock
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @InjectMocks
    private TimetableServiceImpl timetableService;

    private ClassRoom classRoom1;
    private ClassRoom classRoom2;
    private Teacher teacher1;
    private Teacher teacher2;
    private Subject subjectMath;
    private Subject subjectHistory;
    private TeacherClassSubject tcs1;
    private TeacherClassSubject tcs2;
    private TeacherClassSubject tcs3;

    @BeforeEach
    void setUp() {
        AcademicYear year = AcademicYear.builder().id(1).year(2026).active(true).build();

        classRoom1 = ClassRoom.builder()
                .id(10)
                .name("Turma 101")
                .schoolGrade(SchoolGrade.MEDIO_1)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .active(true)
                .build();

        classRoom2 = ClassRoom.builder()
                .id(20)
                .name("Turma 201")
                .schoolGrade(SchoolGrade.MEDIO_2)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .active(true)
                .build();

        teacher1 = Teacher.builder().id(101).name("Prof. Carlos").active(true).build();
        teacher2 = Teacher.builder().id(102).name("Profa. Mariana").active(true).build();

        subjectMath = Subject.builder().id(1).name("Matemática").active(true).build();
        subjectHistory = Subject.builder().id(2).name("História").active(true).build();

        tcs1 = TeacherClassSubject.builder()
                .id(501)
                .classRoom(classRoom1)
                .teacher(teacher1)
                .subject(subjectMath)
                .active(true)
                .build();

        tcs2 = TeacherClassSubject.builder()
                .id(502)
                .classRoom(classRoom1)
                .teacher(teacher2)
                .subject(subjectHistory)
                .active(true)
                .build();

        tcs3 = TeacherClassSubject.builder()
                .id(503)
                .classRoom(classRoom2)
                .teacher(teacher1) // Carlos também dá aula na Turma 201
                .subject(subjectMath)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Sucesso ao adicionar período sem conflitos")
    void testAddSlotSuccess() {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                10, 501, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), 1
        );

        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(teacherClassSubjectRepository.findById(501)).thenReturn(Optional.of(tcs1));
        when(timetableSlotRepository.findClassRoomCollisions(10, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), null))
                .thenReturn(List.of());
        when(timetableSlotRepository.findTeacherCollisions(101, 1, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), null))
                .thenReturn(List.of());

        TimetableSlot savedSlot = TimetableSlot.builder()
                .id(1)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs1)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build();

        when(timetableSlotRepository.save(any(TimetableSlot.class))).thenReturn(savedSlot);

        TimetableSlotResponseDTO result = timetableService.addSlot(dto);

        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(10, result.classRoomId());
        assertEquals("Prof. Carlos", result.teacherName());
        assertEquals("Matemática", result.subjectName());
        assertEquals(DayOfWeekEnum.SEGUNDA, result.dayOfWeek());
        assertEquals("Segunda-feira", result.dayOfWeekLabel());
    }

    @Test
    @DisplayName("Falha ao adicionar horário com choque de turma (duas matérias no mesmo horário)")
    void testAddSlotClassRoomConflict() {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                10, 502, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), 1
        );

        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(teacherClassSubjectRepository.findById(502)).thenReturn(Optional.of(tcs2));

        TimetableSlot existingMathSlot = TimetableSlot.builder()
                .id(1)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs1)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build();

        when(timetableSlotRepository.findClassRoomCollisions(10, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), null))
                .thenReturn(List.of(existingMathSlot));

        ConflictException ex = assertThrows(ConflictException.class, () -> timetableService.addSlot(dto));
        assertTrue(ex.getMessage().contains("Choque de horário na turma"));
        assertTrue(ex.getMessage().contains("Matemática"));

        verify(timetableSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Falha ao adicionar horário com choque de professor (professor alocado em duas turmas no mesmo horário)")
    void testAddSlotTeacherConflict() {
        // Tentando alocar Carlos na Turma 201 às 07:30, mas ele já tem aula na Turma 101 às 07:30
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                20, 503, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), 1
        );

        when(classRoomRepository.findById(20)).thenReturn(Optional.of(classRoom2));
        when(teacherClassSubjectRepository.findById(503)).thenReturn(Optional.of(tcs3));

        when(timetableSlotRepository.findClassRoomCollisions(20, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), null))
                .thenReturn(List.of());

        TimetableSlot existingInClass1 = TimetableSlot.builder()
                .id(1)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs1)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build();

        when(timetableSlotRepository.findTeacherCollisions(101, 1, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), null))
                .thenReturn(List.of(existingInClass1));

        ConflictException ex = assertThrows(ConflictException.class, () -> timetableService.addSlot(dto));
        assertTrue(ex.getMessage().contains("Choque de horário do professor"));
        assertTrue(ex.getMessage().contains("Prof. Carlos"));
        assertTrue(ex.getMessage().contains("Turma 101"));

        verify(timetableSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Horário de término anterior ou igual ao início deve lançar IllegalArgumentException")
    void testInvalidTimeRange() {
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                10, 501, DayOfWeekEnum.SEGUNDA, LocalTime.of(8, 20), LocalTime.of(7, 30), 1
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> timetableService.addSlot(dto));
        assertTrue(ex.getMessage().contains("horário de início deve ser"));
    }

    @Test
    @DisplayName("TCS não pertencente à turma informada deve lançar IllegalArgumentException")
    void testTcsMismatchClassroom() {
        // Passando classRoomId = 10 mas tcs3 pertence à classRoom 20
        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                10, 503, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), 1
        );

        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(teacherClassSubjectRepository.findById(503)).thenReturn(Optional.of(tcs3));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> timetableService.addSlot(dto));
        assertTrue(ex.getMessage().contains("não pertence à turma"));
    }

    @Test
    @DisplayName("Remover horário inexistente lança EntityNotFoundException")
    void testRemoveNotFound() {
        when(timetableSlotRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> timetableService.removeSlot(999));
    }

    @Test
    @DisplayName("Remover horário existente com sucesso")
    void testRemoveSuccess() {
        TimetableSlot slot = TimetableSlot.builder().id(5).build();
        when(timetableSlotRepository.findById(5)).thenReturn(Optional.of(slot));

        timetableService.removeSlot(5);

        verify(timetableSlotRepository).delete(slot);
    }

    @Test
    @DisplayName("Obter grade da turma agrupa corretamente os dias da semana")
    void testGetScheduleByClassroom() {
        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));

        TimetableSlot slot1 = TimetableSlot.builder()
                .id(1)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs1)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build();

        TimetableSlot slot2 = TimetableSlot.builder()
                .id(2)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs2)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(8, 20))
                .endTime(LocalTime.of(9, 10))
                .slotOrder(2)
                .build();

        when(timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(10))
                .thenReturn(List.of(slot1, slot2));

        WeeklyScheduleDTO schedule = timetableService.getScheduleByClassroom(10);

        assertNotNull(schedule);
        assertEquals(10, schedule.classRoomId());
        assertEquals("Turma 101", schedule.classRoomName());
        assertEquals(2, schedule.slots().size());
        assertEquals(2, schedule.slotsByDay().get(DayOfWeekEnum.SEGUNDA).size());
        assertEquals(0, schedule.slotsByDay().get(DayOfWeekEnum.TERCA).size());
    }

    @Test
    @DisplayName("Obter grade do aluno autenticado busca a turma ativa")
    void testGetScheduleForAuthenticatedStudent() {
        Student student = Student.builder().id(301).name("Lucas Almeida").build();
        Enrollment enrollment = Enrollment.builder()
                .id(1)
                .student(student)
                .classRoom(classRoom1)
                .active(true)
                .build();

        when(authenticatedUserService.getAuthenticatedStudent()).thenReturn(student);
        when(enrollmentRepository.findByStudentAndActiveTrue(student)).thenReturn(List.of(enrollment));
        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(10))
                .thenReturn(List.of());

        WeeklyScheduleDTO schedule = timetableService.getScheduleForAuthenticatedStudent();

        assertNotNull(schedule);
        assertEquals(10, schedule.classRoomId());
        assertEquals("Turma 101", schedule.classRoomName());
    }

    @Test
    @DisplayName("Obter grade do professor autenticado busca suas aulas do ano letivo ativo")
    void testGetScheduleForAuthenticatedTeacher() {
        when(authenticatedUserService.getAuthenticatedTeacher()).thenReturn(teacher1);
        when(teacherRepository.findById(101)).thenReturn(Optional.of(teacher1));
        AcademicYear activeYear = AcademicYear.builder().id(1).year(2026).active(true).build();
        when(academicYearRepository.findByActiveTrue()).thenReturn(Optional.of(activeYear));
        when(timetableSlotRepository.findByTeacherAndAcademicYear(101, 1))
                .thenReturn(List.of());

        WeeklyScheduleDTO schedule = timetableService.getScheduleForAuthenticatedTeacher();

        assertNotNull(schedule);
        assertEquals(101, schedule.teacherId());
        assertEquals("Prof. Carlos", schedule.teacherName());
    }

    @Test
    @DisplayName("Sucesso ao atualizar período existente na grade")
    void testUpdateSlotSuccess() {
        TimetableSlot existingSlot = TimetableSlot.builder()
                .id(1)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs1)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build();

        TimetableSlotRequestDTO updateDto = new TimetableSlotRequestDTO(
                10, 501, DayOfWeekEnum.TERCA, LocalTime.of(8, 20), LocalTime.of(9, 10), 2
        );

        when(timetableSlotRepository.findById(1)).thenReturn(Optional.of(existingSlot));
        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(teacherClassSubjectRepository.findById(501)).thenReturn(Optional.of(tcs1));
        when(timetableSlotRepository.findClassRoomCollisions(10, DayOfWeekEnum.TERCA, LocalTime.of(8, 20), LocalTime.of(9, 10), 1))
                .thenReturn(List.of());
        when(timetableSlotRepository.findTeacherCollisions(101, 1, DayOfWeekEnum.TERCA, LocalTime.of(8, 20), LocalTime.of(9, 10), 1))
                .thenReturn(List.of());
        when(timetableSlotRepository.save(any(TimetableSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TimetableSlotResponseDTO result = timetableService.updateSlot(1, updateDto);

        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals(DayOfWeekEnum.TERCA, result.dayOfWeek());
        assertEquals(LocalTime.of(8, 20), result.startTime());
        assertEquals(LocalTime.of(9, 10), result.endTime());
        assertEquals(2, result.slotOrder());
    }

    @Test
    @DisplayName("Atualização falha quando ocorre choque de turma")
    void testUpdateSlotClassRoomConflict() {
        TimetableSlot existingSlot = TimetableSlot.builder()
                .id(1)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs1)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build();

        TimetableSlot conflictingSlot = TimetableSlot.builder()
                .id(2)
                .classRoom(classRoom1)
                .teacherClassSubject(tcs2)
                .dayOfWeek(DayOfWeekEnum.TERCA)
                .startTime(LocalTime.of(8, 20))
                .endTime(LocalTime.of(9, 10))
                .slotOrder(2)
                .build();

        TimetableSlotRequestDTO updateDto = new TimetableSlotRequestDTO(
                10, 501, DayOfWeekEnum.TERCA, LocalTime.of(8, 20), LocalTime.of(9, 10), 2
        );

        when(timetableSlotRepository.findById(1)).thenReturn(Optional.of(existingSlot));
        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(teacherClassSubjectRepository.findById(501)).thenReturn(Optional.of(tcs1));
        when(timetableSlotRepository.findClassRoomCollisions(10, DayOfWeekEnum.TERCA, LocalTime.of(8, 20), LocalTime.of(9, 10), 1))
                .thenReturn(List.of(conflictingSlot));

        ConflictException ex = assertThrows(ConflictException.class, () -> timetableService.updateSlot(1, updateDto));
        assertTrue(ex.getMessage().contains("Choque de horário na turma"));
        verify(timetableSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Professor pode lecionar no mesmo horário em anos letivos distintos sem falso choque")
    void testTeacherCanTeachSameTimeInDifferentAcademicYears() {
        // classRoom1 pertence ao ano 1 (2026), classRoomPast pertence ao ano 99 (2025)
        AcademicYear pastYear = AcademicYear.builder().id(99).year(2025).active(false).build();
        ClassRoom classRoomPast = ClassRoom.builder()
                .id(90)
                .name("Turma 2025")
                .schoolGrade(SchoolGrade.MEDIO_1)
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(pastYear)
                .active(true)
                .build();

        TeacherClassSubject tcsCurrentYear = TeacherClassSubject.builder()
                .id(501)
                .classRoom(classRoom1)
                .teacher(teacher1)
                .subject(subjectMath)
                .active(true)
                .build();

        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                10, 501, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), 1
        );

        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(teacherClassSubjectRepository.findById(501)).thenReturn(Optional.of(tcsCurrentYear));
        when(timetableSlotRepository.findClassRoomCollisions(10, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), null))
                .thenReturn(List.of());

        // findTeacherCollisions é chamado com academicYearId = 1 (ano da turma atual), NÃO colidindo com o ano 99
        when(timetableSlotRepository.findTeacherCollisions(101, 1, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), null))
                .thenReturn(List.of());

        TimetableSlot savedSlot = TimetableSlot.builder()
                .id(10)
                .classRoom(classRoom1)
                .teacherClassSubject(tcsCurrentYear)
                .dayOfWeek(DayOfWeekEnum.SEGUNDA)
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 20))
                .slotOrder(1)
                .build();
        when(timetableSlotRepository.save(any(TimetableSlot.class))).thenReturn(savedSlot);

        TimetableSlotResponseDTO result = timetableService.addSlot(dto);
        assertNotNull(result);
        assertEquals(10, result.id());
    }

    @Test
    @DisplayName("Tentativa de agendamento em turma inativa deve lançar IllegalArgumentException")
    void testInactiveClassRoomThrowsException() {
        ClassRoom inactiveRoom = ClassRoom.builder().id(30).name("Turma Inativa").active(false).build();
        TeacherClassSubject tcs = TeacherClassSubject.builder().id(601).classRoom(inactiveRoom).active(true).build();

        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                30, 601, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), 1
        );

        when(classRoomRepository.findById(30)).thenReturn(Optional.of(inactiveRoom));
        when(teacherClassSubjectRepository.findById(601)).thenReturn(Optional.of(tcs));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> timetableService.addSlot(dto));
        assertTrue(ex.getMessage().contains("turma inativa"));
    }

    @Test
    @DisplayName("Tentativa de agendamento em vinculação TCS inativa deve lançar IllegalArgumentException")
    void testInactiveTcsThrowsException() {
        TeacherClassSubject inactiveTcs = TeacherClassSubject.builder()
                .id(602)
                .classRoom(classRoom1)
                .teacher(teacher1)
                .subject(subjectMath)
                .active(false)
                .build();

        TimetableSlotRequestDTO dto = new TimetableSlotRequestDTO(
                10, 602, DayOfWeekEnum.SEGUNDA, LocalTime.of(7, 30), LocalTime.of(8, 20), 1
        );

        when(classRoomRepository.findById(10)).thenReturn(Optional.of(classRoom1));
        when(teacherClassSubjectRepository.findById(602)).thenReturn(Optional.of(inactiveTcs));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> timetableService.addSlot(dto));
        assertTrue(ex.getMessage().contains("está inativa"));
    }
}
