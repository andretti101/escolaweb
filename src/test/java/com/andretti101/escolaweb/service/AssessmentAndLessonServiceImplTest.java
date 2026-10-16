package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.AssessmentType;
import com.andretti101.escolaweb.model.enums.LessonCount;
import com.andretti101.escolaweb.repository.AssessmentRepository;
import com.andretti101.escolaweb.repository.AttendanceHistoryRepository;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import com.andretti101.escolaweb.repository.LessonRepository;
import com.andretti101.escolaweb.service.impl.AssessmentServiceImpl;
import com.andretti101.escolaweb.service.impl.LessonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentAndLessonServiceImplTest {

    @Nested
    @DisplayName("AssessmentServiceImpl Unit Tests")
    class AssessmentServiceTests {

        @Mock
        private AssessmentRepository assessmentRepository;
        @Mock
        private GradeRepository gradeRepository;
        @Mock
        private TeacherClassSubjectService teacherClassSubjectService;
        @Mock
        private AcademicPeriodService academicPeriodService;

        @InjectMocks
        private AssessmentServiceImpl assessmentService;

        private AcademicYear year2026;
        private ClassRoom classRoom;
        private Subject subject;
        private TeacherClassSubject tcs;
        private AcademicPeriod period;

        @BeforeEach
        void setUp() {
            year2026 = AcademicYear.builder().id(1).year(2026).active(true).build();
            classRoom = ClassRoom.builder()
                    .id(10)
                    .name("1º Ano A")
                    .academicYear(year2026)
                    .periodType(AcademicPeriodType.BIMESTER)
                    .active(true)
                    .build();
            subject = Subject.builder().id(5).name("Física").active(true).build();
            tcs = TeacherClassSubject.builder()
                    .id(100)
                    .classRoom(classRoom)
                    .subject(subject)
                    .minAssessmentsPerPeriod(1)
                    .maxAssessmentsPerPeriod(2)
                    .active(true)
                    .build();
            period = AcademicPeriod.builder()
                    .id(200)
                    .name("1º Bimestre")
                    .academicYear(year2026)
                    .periodType(AcademicPeriodType.BIMESTER)
                    .startDate(LocalDate.of(2026, 2, 1))
                    .endDate(LocalDate.of(2026, 4, 30))
                    .closed(false)
                    .build();
        }

        @Test
        @DisplayName("create salva avaliação quando ano, tipo de período, intervalo de datas e limite máximo são válidos")
        void create_validAssessment_savesSuccessfully() {
            Assessment toCreate = Assessment.builder()
                    .title("Prova Mensal")
                    .date(LocalDate.of(2026, 3, 15))
                    .assessmentType(AssessmentType.EXAM)
                    .teacherClassSubject(tcs)
                    .period(period)
                    .build();

            when(teacherClassSubjectService.findById(100)).thenReturn(tcs);
            when(academicPeriodService.findById(200)).thenReturn(period);
            when(assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, period)).thenReturn(1L);
            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

            Assessment saved = assessmentService.create(toCreate);
            assertEquals("Prova Mensal", saved.getTitle());
            verify(assessmentRepository).save(toCreate);
        }

        @Test
        @DisplayName("create rejeita conflito de ano letivo, conflito de periodType, período fechado, data fora do período e limite máximo atingido")
        void create_validationErrors_throwsExpectedExceptions() {
            Assessment toCreate = Assessment.builder()
                    .title("Prova")
                    .date(LocalDate.of(2026, 3, 15))
                    .assessmentType(AssessmentType.EXAM)
                    .teacherClassSubject(tcs)
                    .period(period)
                    .build();

            when(teacherClassSubjectService.findById(100)).thenReturn(tcs);
            when(academicPeriodService.findById(200)).thenReturn(period);

            // 1. Limite máximo atingido
            when(assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, period)).thenReturn(2L);
            assertThrows(IllegalStateException.class, () -> assessmentService.create(toCreate));

            // 2. Data antes do início do período
            toCreate.setDate(LocalDate.of(2026, 1, 20));
            assertThrows(IllegalArgumentException.class, () -> assessmentService.create(toCreate));

            // 3. Data após o fim do período
            toCreate.setDate(LocalDate.of(2026, 5, 5));
            assertThrows(IllegalArgumentException.class, () -> assessmentService.create(toCreate));

            // 4. Período fechado
            toCreate.setDate(LocalDate.of(2026, 3, 15));
            period.setClosed(true);
            assertThrows(IllegalStateException.class, () -> assessmentService.create(toCreate));

            // 5. Conflito de PeriodType
            period.setClosed(false);
            period.setPeriodType(AcademicPeriodType.TRIMESTER);
            assertThrows(IllegalStateException.class, () -> assessmentService.create(toCreate));

            // 6. Conflito de AcademicYear
            period.setPeriodType(AcademicPeriodType.BIMESTER);
            period.setAcademicYear(AcademicYear.builder().id(99).year(2025).build());
            assertThrows(IllegalStateException.class, () -> assessmentService.create(toCreate));
        }

        @Test
        @DisplayName("delete impede exclusão quando o período já está fechado")
        void delete_closedPeriod_throwsIllegalState() {
            period.setClosed(true);
            Assessment existing = Assessment.builder().id(50).period(period).teacherClassSubject(tcs).build();
            when(assessmentRepository.findById(50)).thenReturn(Optional.of(existing));

            assertThrows(IllegalStateException.class, () -> assessmentService.delete(50));
            verify(assessmentRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("LessonServiceImpl Unit Tests")
    class LessonServiceTests {

        @Mock
        private LessonRepository lessonRepository;
        @Mock
        private AttendanceRepository attendanceRepository;
        @Mock
        private AttendanceHistoryRepository attendanceHistoryRepository;
        @Mock
        private TeacherClassSubjectService teacherClassSubjectService;

        @InjectMocks
        private LessonServiceImpl lessonService;

        @Test
        @DisplayName("create exige lessonDate e atribuição válida")
        void create_validatesDateAndTcs() {
            TeacherClassSubject tcs = TeacherClassSubject.builder().id(10).build();
            Lesson noDate = Lesson.builder().teacherClassSubject(tcs).build();
            assertThrows(IllegalArgumentException.class, () -> lessonService.create(noDate));

            Lesson valid = Lesson.builder()
                    .lessonDate(LocalDate.of(2026, 3, 12))
                    .content("Cinemática")
                    .lessonCount(LessonCount.TWO)
                    .teacherClassSubject(tcs)
                    .build();
            when(teacherClassSubjectService.findById(10)).thenReturn(tcs);
            when(lessonRepository.save(valid)).thenReturn(valid);

            Lesson created = lessonService.create(valid);
            assertEquals("Cinemática", created.getContent());
        }

        @Test
        @DisplayName("delete remove em cascata AttendanceHistory e Attendance antes de excluir a aula")
        void delete_cascadesAttendancesAndHistories() {
            Lesson lesson = Lesson.builder().id(80).lessonDate(LocalDate.of(2026, 3, 12)).build();
            AttendanceHistory h1 = AttendanceHistory.builder().id(1).build();
            Attendance att = Attendance.builder()
                    .id(500)
                    .lesson(lesson)
                    .history(List.of(h1))
                    .build();

            when(lessonRepository.findById(80)).thenReturn(Optional.of(lesson));
            when(attendanceRepository.findByLesson(lesson)).thenReturn(List.of(att));

            lessonService.delete(80);

            verify(attendanceHistoryRepository).deleteAll(List.of(h1));
            verify(attendanceRepository).deleteAll(List.of(att));
            verify(lessonRepository).deleteById(80);
        }
    }
}
