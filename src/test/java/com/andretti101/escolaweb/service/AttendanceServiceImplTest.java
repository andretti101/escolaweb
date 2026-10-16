package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.StudentAttendanceReportDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import com.andretti101.escolaweb.model.enums.LessonCount;
import com.andretti101.escolaweb.repository.AttendanceHistoryRepository;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.LessonRepository;
import com.andretti101.escolaweb.service.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private AttendanceHistoryRepository attendanceHistoryRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonService lessonService;
    @Mock
    private StudentService studentService;
    @Mock
    private TeacherClassSubjectService teacherClassSubjectService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Student student;
    private AcademicYear activeYear;
    private ClassRoom classRoom;
    private Subject mathSubject;
    private TeacherClassSubject tcs;
    private Lesson lesson1;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(11).name("Lucas Almeida").active(true).build();
        activeYear = AcademicYear.builder().id(2026).year(2026).active(true).build();
        classRoom = ClassRoom.builder().id(1).name("1º Ano A").academicYear(activeYear).active(true).build();
        mathSubject = Subject.builder().id(3).name("Matemática").active(true).build();
        tcs = TeacherClassSubject.builder().id(10).classRoom(classRoom).subject(mathSubject).active(true).build();
        lesson1 = Lesson.builder()
                .id(100)
                .lessonDate(LocalDate.of(2026, 3, 10))
                .lessonCount(LessonCount.TWO)
                .teacherClassSubject(tcs)
                .build();
    }

    @Test
    @DisplayName("register salva presença e grava histórico inicial com previousStatus nulo")
    void register_validAttendance_savesAndRecordsHistory() {
        Attendance toRegister = Attendance.builder()
                .lesson(lesson1)
                .student(student)
                .status(AttendanceStatus.PRESENT)
                .build();

        when(lessonService.findById(100)).thenReturn(lesson1);
        when(studentService.findById(11)).thenReturn(student);
        when(attendanceRepository.existsByLessonAndStudent(lesson1, student)).thenReturn(false);
        when(enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, classRoom)).thenReturn(true);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> {
            Attendance a = inv.getArgument(0);
            a.setId(700);
            return a;
        });

        Attendance saved = attendanceService.register(toRegister);

        assertEquals(700, saved.getId());
        ArgumentCaptor<AttendanceHistory> captor = ArgumentCaptor.forClass(AttendanceHistory.class);
        verify(attendanceHistoryRepository).save(captor.capture());
        assertNull(captor.getValue().getPreviousStatus());
        assertEquals("PRESENT", captor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("register lança exceção quando status é nulo, presença duplicada ou aluno sem matrícula ativa")
    void register_validationFailures_throwsExpectedExceptions() {
        when(lessonService.findById(100)).thenReturn(lesson1);
        when(studentService.findById(11)).thenReturn(student);

        // Status nulo
        Attendance nullStatus = Attendance.builder().lesson(lesson1).student(student).status(null).build();
        assertThrows(IllegalArgumentException.class, () -> attendanceService.register(nullStatus));

        // Presença duplicada
        Attendance valid = Attendance.builder().lesson(lesson1).student(student).status(AttendanceStatus.ABSENT).build();
        when(attendanceRepository.existsByLessonAndStudent(lesson1, student)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> attendanceService.register(valid));

        // Sem matrícula ativa
        when(attendanceRepository.existsByLessonAndStudent(lesson1, student)).thenReturn(false);
        when(enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, classRoom)).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> attendanceService.register(valid));
    }

    @Test
    @DisplayName("update grava AttendanceHistory apenas quando o status muda")
    void update_recordsHistoryOnlyWhenStatusChanges() {
        Attendance existing = Attendance.builder()
                .id(700)
                .lesson(lesson1)
                .student(student)
                .status(AttendanceStatus.ABSENT)
                .build();
        when(attendanceRepository.findById(700)).thenReturn(Optional.of(existing));

        // Mesmo status -> sem histórico
        attendanceService.update(700, Attendance.builder().status(AttendanceStatus.ABSENT).build());
        verify(attendanceHistoryRepository, never()).save(any());

        // Status alterado -> grava histórico
        attendanceService.update(700, Attendance.builder().status(AttendanceStatus.JUSTIFIED_ABSENCE).build());
        ArgumentCaptor<AttendanceHistory> captor = ArgumentCaptor.forClass(AttendanceHistory.class);
        verify(attendanceHistoryRepository).save(captor.capture());
        assertEquals("ABSENT", captor.getValue().getPreviousStatus());
        assertEquals("JUSTIFIED_ABSENCE", captor.getValue().getNewStatus());
    }

    @Test
    @DisplayName("calculateFrequency pondera por LessonCount e considera JUSTIFIED_ABSENCE como presença válida")
    void calculateFrequency_weightsByLessonCountAndTreatsJustifiedAsValid() {
        when(studentService.findById(11)).thenReturn(student);
        when(teacherClassSubjectService.findById(10)).thenReturn(tcs);

        // Sem aulas -> 100%
        when(lessonRepository.findByTeacherClassSubject(tcs)).thenReturn(List.of());
        assertEquals(BigDecimal.valueOf(100), attendanceService.calculateFrequency(11, 10));

        // Aula 1 (2 tempos): PRESENT (+2 válidos)
        // Aula 2 (1 tempo): JUSTIFIED_ABSENCE (+1 válido)
        // Aula 3 (1 tempo): ABSENT (+1 falta não justificada)
        // Total = 3 válidos / 4 tempos = 75.00%
        Lesson lesson2 = Lesson.builder().id(101).lessonCount(LessonCount.ONE).teacherClassSubject(tcs).build();
        Lesson lesson3 = Lesson.builder().id(102).lessonCount(LessonCount.ONE).teacherClassSubject(tcs).build();
        when(lessonRepository.findByTeacherClassSubject(tcs)).thenReturn(List.of(lesson1, lesson2, lesson3));

        Attendance a1 = Attendance.builder().lesson(lesson1).student(student).status(AttendanceStatus.PRESENT).build();
        Attendance a2 = Attendance.builder().lesson(lesson2).student(student).status(AttendanceStatus.JUSTIFIED_ABSENCE).build();
        Attendance a3 = Attendance.builder().lesson(lesson3).student(student).status(AttendanceStatus.ABSENT).build();
        when(attendanceRepository.findByStudent(student)).thenReturn(List.of(a1, a2, a3));

        BigDecimal freq = attendanceService.calculateFrequency(11, 10);
        assertEquals(new BigDecimal("75.00"), freq);
    }

    @Test
    @DisplayName("getStudentAttendanceReport agrega faltas e percentuais por disciplina no ano letivo selecionado")
    void getStudentAttendanceReport_aggregatesBySubjectAndYear() {
        when(studentService.findById(11)).thenReturn(student);

        Lesson lesson2 = Lesson.builder().id(101).lessonCount(LessonCount.TWO).teacherClassSubject(tcs).build();
        Attendance a1 = Attendance.builder().lesson(lesson1).student(student).status(AttendanceStatus.PRESENT).build();
        Attendance a2 = Attendance.builder().lesson(lesson2).student(student).status(AttendanceStatus.ABSENT).build();

        when(attendanceRepository.findByStudent(student)).thenReturn(List.of(a1, a2));

        StudentAttendanceReportDTO report = attendanceService.getStudentAttendanceReport(11, 2026);

        assertEquals(11, report.studentId());
        assertEquals(new BigDecimal("50.00"), report.generalFrequency());
        assertEquals(2, report.absencesPerSubject().get("Matemática"));
        assertEquals(new BigDecimal("50.00"), report.frequencyPerSubject().get("Matemática"));
    }
}
