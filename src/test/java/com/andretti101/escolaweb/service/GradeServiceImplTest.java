package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.GradeHistoryRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import com.andretti101.escolaweb.service.impl.GradeServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private GradeHistoryRepository gradeHistoryRepository;
    @Mock
    private AuthenticatedUserService authenticatedUserService;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private AssessmentService assessmentService;
    @Mock
    private SchoolSettingsService schoolSettingsService;
    @Mock
    private TeacherClassSubjectService teacherClassSubjectService;
    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private GradeServiceImpl gradeService;

    private Student student;
    private ClassRoom classRoom;
    private TeacherClassSubject tcs;
    private AcademicPeriod period1;
    private AcademicPeriod period2;
    private Assessment assessment1;
    private SchoolSettings settings;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(11).name("Lucas Almeida").active(true).build();
        classRoom = ClassRoom.builder()
                .id(1)
                .name("1º Ano A")
                .periodType(AcademicPeriodType.SEMESTER)
                .active(true)
                .build();
        tcs = TeacherClassSubject.builder()
                .id(10)
                .classRoom(classRoom)
                .active(true)
                .build();
        period1 = AcademicPeriod.builder()
                .id(101)
                .name("1º Semestre")
                .periodType(AcademicPeriodType.SEMESTER)
                .closed(false)
                .build();
        period2 = AcademicPeriod.builder()
                .id(102)
                .name("2º Semestre")
                .periodType(AcademicPeriodType.SEMESTER)
                .closed(false)
                .build();
        assessment1 = Assessment.builder()
                .id(50)
                .title("Prova 1")
                .teacherClassSubject(tcs)
                .period(period1)
                .build();
        settings = SchoolSettings.builder()
                .minimumGrade(new BigDecimal("6.0"))
                .minimumAttendance(new BigDecimal("75.00"))
                .build();
    }

    @Test
    @DisplayName("launch salva nota e define situação PENDING quando nem todos os períodos da divisão têm notas")
    void launch_validGradeWithIncompletePeriods_setsPendingSituation() {
        Grade toLaunch = Grade.builder()
                .student(student)
                .assessment(assessment1)
                .value(new BigDecimal("8.5"))
                .build();

        when(studentService.findById(11)).thenReturn(student);
        when(assessmentService.findById(50)).thenReturn(assessment1);
        when(gradeRepository.existsByStudentAndAssessment(student, assessment1)).thenReturn(false);
        when(enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, classRoom)).thenReturn(true);
        when(gradeRepository.save(any(Grade.class))).thenAnswer(inv -> {
            Grade g = inv.getArgument(0);
            g.setId(500);
            return g;
        });
        when(schoolSettingsService.findSettings()).thenReturn(settings);
        when(gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs))
                .thenReturn(List.of(toLaunch));

        Grade saved = gradeService.launch(toLaunch);

        assertEquals(500, saved.getId());
        assertEquals(StudentSituation.PENDING, saved.getStudentSituation());
        verifyNoInteractions(gradeHistoryRepository);
    }

    @Test
    @DisplayName("launch define situação APPROVED quando todos os períodos têm notas e média/frequência atingem o mínimo")
    void launch_allPeriodsCompleteAndPassing_setsApprovedSituation() {
        Assessment assessment2 = Assessment.builder()
                .id(51)
                .title("Prova 2")
                .teacherClassSubject(tcs)
                .period(period2)
                .build();
        Grade g1 = Grade.builder().id(1).student(student).assessment(assessment1).value(new BigDecimal("7.0")).build();
        Grade g2 = Grade.builder().id(2).student(student).assessment(assessment2).value(new BigDecimal("8.0")).build();

        when(studentService.findById(11)).thenReturn(student);
        when(assessmentService.findById(51)).thenReturn(assessment2);
        when(teacherClassSubjectService.findById(10)).thenReturn(tcs);
        when(gradeRepository.existsByStudentAndAssessment(student, assessment2)).thenReturn(false);
        when(enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, classRoom)).thenReturn(true);
        when(gradeRepository.save(any(Grade.class))).thenReturn(g2);
        when(schoolSettingsService.findSettings()).thenReturn(settings);
        when(gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs))
                .thenReturn(List.of(g1, g2));
        when(attendanceService.calculateFrequency(11, 10)).thenReturn(new BigDecimal("85.00"));

        Grade result = gradeService.launch(g2);

        assertEquals(StudentSituation.APPROVED, result.getStudentSituation());
        assertEquals(StudentSituation.APPROVED, g1.getStudentSituation());
    }

    @Test
    @DisplayName("launch define REPROVED_BY_GRADE quando média é inferior ao mínimo e REPROVED_BY_ATTENDANCE quando frequência é insuficiente")
    void launch_allPeriodsComplete_setsReprovedByGradeOrAttendance() {
        Assessment assessment2 = Assessment.builder()
                .id(51)
                .title("Prova 2")
                .teacherClassSubject(tcs)
                .period(period2)
                .build();
        Grade g1 = Grade.builder().id(1).student(student).assessment(assessment1).value(new BigDecimal("4.0")).build();
        Grade g2 = Grade.builder().id(2).student(student).assessment(assessment2).value(new BigDecimal("5.0")).build();

        when(studentService.findById(11)).thenReturn(student);
        when(assessmentService.findById(51)).thenReturn(assessment2);
        when(teacherClassSubjectService.findById(10)).thenReturn(tcs);
        when(gradeRepository.existsByStudentAndAssessment(student, assessment2)).thenReturn(false);
        when(enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, classRoom)).thenReturn(true);
        when(gradeRepository.save(any(Grade.class))).thenReturn(g2);
        when(schoolSettingsService.findSettings()).thenReturn(settings);
        when(gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs))
                .thenReturn(List.of(g1, g2));

        // 1. Média 4.50 < 6.0 -> FAILED
        when(attendanceService.calculateFrequency(11, 10)).thenReturn(new BigDecimal("90.00"));
        Grade reprovedGrade = gradeService.launch(g2);
        assertEquals(StudentSituation.FAILED, reprovedGrade.getStudentSituation());

        // 2. Média 7.50 >= 6.0, mas frequência 60.00 < 75.00 -> FAILED
        g1.setValue(new BigDecimal("7.0"));
        g2.setValue(new BigDecimal("8.0"));
        when(attendanceService.calculateFrequency(11, 10)).thenReturn(new BigDecimal("60.00"));
        Grade reprovedAtt = gradeService.launch(g2);
        assertEquals(StudentSituation.FAILED, reprovedAtt.getStudentSituation());
    }

    @Test
    @DisplayName("launch lança exceções para nota duplicada, aluno sem matrícula ativa, período fechado ou nota fora de 0..10")
    void launch_validationErrors_throwsExpectedExceptions() {
        Grade toLaunch = Grade.builder()
                .student(student)
                .assessment(assessment1)
                .value(new BigDecimal("8.0"))
                .build();

        when(studentService.findById(11)).thenReturn(student);
        when(assessmentService.findById(50)).thenReturn(assessment1);

        // 1. Nota duplicada
        when(gradeRepository.existsByStudentAndAssessment(student, assessment1)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> gradeService.launch(toLaunch));

        // 2. Sem matrícula ativa
        when(gradeRepository.existsByStudentAndAssessment(student, assessment1)).thenReturn(false);
        when(enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, classRoom)).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> gradeService.launch(toLaunch));

        // 3. Período fechado
        when(enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, classRoom)).thenReturn(true);
        period1.setClosed(true);
        assertThrows(IllegalStateException.class, () -> gradeService.launch(toLaunch));

        // 4. Valor fora do intervalo (< 0 ou > 10)
        period1.setClosed(false);
        toLaunch.setValue(new BigDecimal("10.5"));
        assertThrows(IllegalArgumentException.class, () -> gradeService.launch(toLaunch));

        toLaunch.setValue(new BigDecimal("-0.1"));
        assertThrows(IllegalArgumentException.class, () -> gradeService.launch(toLaunch));
    }

    @Test
    @DisplayName("update grava GradeHistory apenas quando o valor numérico muda e faz trim nas notas")
    void update_recordsHistoryOnlyWhenValueChanges() {
        Grade existing = Grade.builder()
                .id(500)
                .student(student)
                .assessment(assessment1)
                .value(new BigDecimal("6.00"))
                .build();

        when(gradeRepository.findById(500)).thenReturn(Optional.of(existing));
        when(schoolSettingsService.findSettings()).thenReturn(settings);
        when(gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs))
                .thenReturn(List.of(existing));

        // Mesmo valor numérico (6.0 vs 6.00) -> não grava histórico
        Grade sameValue = Grade.builder().value(new BigDecimal("6.0")).build();
        gradeService.update(500, sameValue, "Observação ignorada");
        verify(gradeHistoryRepository, never()).save(any());

        // Valor alterado -> grava histórico com observação limpa
        Grade newValue = Grade.builder().value(new BigDecimal("9.25")).build();
        gradeService.update(500, newValue, "  Revisão de prova  ");

        ArgumentCaptor<GradeHistory> captor = ArgumentCaptor.forClass(GradeHistory.class);
        verify(gradeHistoryRepository).save(captor.capture());
        GradeHistory history = captor.getValue();
        assertEquals(0, new BigDecimal("6.00").compareTo(history.getPreviousGrade()));
        assertEquals(0, new BigDecimal("9.25").compareTo(history.getNewGrade()));
        assertEquals("Revisão de prova", history.getNotes());
    }

    @Test
    @DisplayName("calculateAverage agrupa por período, calcula média de cada período e aplica arredondamento CEILING")
    void calculateAverage_groupsByPeriodAndRoundsCeiling() {
        when(studentService.findById(11)).thenReturn(student);
        when(teacherClassSubjectService.findById(10)).thenReturn(tcs);

        // Sem notas -> 0
        when(gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs)).thenReturn(List.of());
        assertEquals(BigDecimal.ZERO, gradeService.calculateAverage(11, 10));

        // Com notas em 2 períodos: P1=(7.0 + 8.0)/2 = 7.5000; P2=(6.0)/1 = 6.0000 -> (7.5 + 6.0)/2 = 6.75
        Assessment a1 = Assessment.builder().id(1).period(period1).teacherClassSubject(tcs).build();
        Assessment a2 = Assessment.builder().id(2).period(period1).teacherClassSubject(tcs).build();
        Assessment a3 = Assessment.builder().id(3).period(period2).teacherClassSubject(tcs).build();

        Grade g1 = Grade.builder().value(new BigDecimal("7.0")).assessment(a1).build();
        Grade g2 = Grade.builder().value(new BigDecimal("8.0")).assessment(a2).build();
        Grade g3 = Grade.builder().value(new BigDecimal("6.0")).assessment(a3).build();

        when(gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs))
                .thenReturn(List.of(g1, g2, g3));

        BigDecimal avg = gradeService.calculateAverage(11, 10);
        assertEquals(new BigDecimal("6.75"), avg);
    }

    @Test
    @DisplayName("findById e delete lançam EntityNotFoundException quando nota não existe")
    void findByIdAndDelete_whenNotFound_throwsEntityNotFound() {
        when(gradeRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> gradeService.findById(999));
        assertThrows(EntityNotFoundException.class, () -> gradeService.delete(999));
    }
}
