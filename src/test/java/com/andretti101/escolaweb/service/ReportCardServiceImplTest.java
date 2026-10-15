package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.impl.ReportCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportCardServiceImplTest {

    @Mock
    private StudentService studentService;
    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private SchoolSettingsService schoolSettingsService;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private AcademicPeriodRepository academicPeriodRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private TeacherClassSubjectRepository teacherClassSubjectRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private ReportCardServiceImpl reportCardService;

    private AcademicYear year;
    private ClassRoom classRoom;
    private Student student;
    private Enrollment enrollment;
    private TeacherClassSubject tcs;
    private Subject subject;
    private SchoolSettings settings;

    @BeforeEach
    void setUp() {
        year = AcademicYear.builder()
                .id(1)
                .year(2026)
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .build();

        classRoom = ClassRoom.builder()
                .id(10)
                .name("Turma Teste")
                .periodType(AcademicPeriodType.TRIMESTER)
                .academicYear(year)
                .build();

        student = Student.builder()
                .id(50)
                .name("Aluno Teste")
                .registrationNumber("2026050")
                .build();

        enrollment = Enrollment.builder()
                .id(100)
                .student(student)
                .classRoom(classRoom)
                .build();

        subject = Subject.builder()
                .id(1)
                .name("Matemática")
                .build();

        tcs = TeacherClassSubject.builder()
                .id(20)
                .classRoom(classRoom)
                .subject(subject)
                .build();

        settings = SchoolSettings.builder()
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .build();

        when(schoolSettingsService.findSettings()).thenReturn(settings);
        when(teacherClassSubjectRepository.findByClassRoom(classRoom)).thenReturn(List.of(tcs));
        when(attendanceRepository.findByStudentsAndYear(anyList(), eq(year))).thenReturn(List.of());
    }

    @Test
    @DisplayName("Quando todos os períodos estão fechados e aluno não tem notas (closedPeriodsWithGrades == 0), situação DEVE ser FAILED e NUNCA PENDING")
    void testAllPeriodsClosedZeroGradesMustBeFailedNotPending() {
        AcademicPeriod p1 = AcademicPeriod.builder().id(1).name("1º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p2 = AcademicPeriod.builder().id(2).name("2º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p3 = AcademicPeriod.builder().id(3).name("3º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();

        when(academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.TRIMESTER))
                .thenReturn(List.of(p1, p2, p3));
        when(gradeRepository.findByStudentsAndYear(anyList(), eq(year)))
                .thenReturn(List.of()); // Nenhuma nota

        List<ReportCardDTO> cards = reportCardService.generateReportCardsBatch(List.of(enrollment), year);

        assertEquals(1, cards.size());
        ReportCardDTO card = cards.get(0);
        ReportCardDTO.SubjectSituationDTO subj = card.subjects().get(0);

        assertEquals(StudentSituation.FAILED, subj.situation(),
                "Com todos os períodos fechados e nenhuma nota, a disciplina deve estar FAILED");
        assertEquals(StudentSituation.FAILED, card.generalSituation(),
                "A situação geral deve ser FAILED e nunca PENDING");
    }

    @Test
    @DisplayName("Quando todos os períodos estão fechados e a soma atinge a meta, situação deve ser APPROVED")
    void testAllPeriodsClosedTargetReachedMustBeApproved() {
        AcademicPeriod p1 = AcademicPeriod.builder().id(1).name("1º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p2 = AcademicPeriod.builder().id(2).name("2º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p3 = AcademicPeriod.builder().id(3).name("3º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();

        when(academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.TRIMESTER))
                .thenReturn(List.of(p1, p2, p3));

        Assessment a1 = Assessment.builder().id(1).period(p1).teacherClassSubject(tcs).build();
        Assessment a2 = Assessment.builder().id(2).period(p2).teacherClassSubject(tcs).build();
        Assessment a3 = Assessment.builder().id(3).period(p3).teacherClassSubject(tcs).build();

        // 3 períodos x 6.0 meta = 18.0. Notas: 7.0, 7.0, 7.0 = 21.0 >= 18.0
        Grade g1 = Grade.builder().id(1).student(student).assessment(a1).value(BigDecimal.valueOf(7.0)).build();
        Grade g2 = Grade.builder().id(2).student(student).assessment(a2).value(BigDecimal.valueOf(7.0)).build();
        Grade g3 = Grade.builder().id(3).student(student).assessment(a3).value(BigDecimal.valueOf(7.0)).build();

        when(gradeRepository.findByStudentsAndYear(anyList(), eq(year)))
                .thenReturn(List.of(g1, g2, g3));

        List<ReportCardDTO> cards = reportCardService.generateReportCardsBatch(List.of(enrollment), year);

        ReportCardDTO.SubjectSituationDTO subj = cards.get(0).subjects().get(0);
        assertEquals(StudentSituation.APPROVED, subj.situation());
        assertEquals(StudentSituation.APPROVED, cards.get(0).generalSituation());
    }

    @Test
    @DisplayName("Quando restam períodos abertos e o aluno ainda pode atingir a meta, situação deve ser PENDING")
    void testRemainingPeriodsOpenCanStillPassMustBePending() {
        AcademicPeriod p1 = AcademicPeriod.builder().id(1).name("1º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p2 = AcademicPeriod.builder().id(2).name("2º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p3 = AcademicPeriod.builder().id(3).name("3º Tri").closed(false).periodType(AcademicPeriodType.TRIMESTER).build();

        when(academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.TRIMESTER))
                .thenReturn(List.of(p1, p2, p3));

        Assessment a1 = Assessment.builder().id(1).period(p1).teacherClassSubject(tcs).build();
        Assessment a2 = Assessment.builder().id(2).period(p2).teacherClassSubject(tcs).build();

        // Meta 18.0. Notas nos fechados: 5.0 + 5.0 = 10.0. Resta 1 período aberto -> max possible = 10 + 10 = 20.0 >= 18.0
        Grade g1 = Grade.builder().id(1).student(student).assessment(a1).value(BigDecimal.valueOf(5.0)).build();
        Grade g2 = Grade.builder().id(2).student(student).assessment(a2).value(BigDecimal.valueOf(5.0)).build();

        when(gradeRepository.findByStudentsAndYear(anyList(), eq(year)))
                .thenReturn(List.of(g1, g2));

        List<ReportCardDTO> cards = reportCardService.generateReportCardsBatch(List.of(enrollment), year);

        ReportCardDTO.SubjectSituationDTO subj = cards.get(0).subjects().get(0);
        assertEquals(StudentSituation.PENDING, subj.situation());
        assertEquals(StudentSituation.PENDING, cards.get(0).generalSituation());
    }

    @Test
    @DisplayName("Quando restam períodos abertos mas o aluno matematicamente não pode mais atingir a meta, situação deve ser FAILED")
    void testRemainingPeriodsOpenCannotReachTargetMustBeFailed() {
        AcademicPeriod p1 = AcademicPeriod.builder().id(1).name("1º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p2 = AcademicPeriod.builder().id(2).name("2º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p3 = AcademicPeriod.builder().id(3).name("3º Tri").closed(false).periodType(AcademicPeriodType.TRIMESTER).build();

        when(academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.TRIMESTER))
                .thenReturn(List.of(p1, p2, p3));

        Assessment a1 = Assessment.builder().id(1).period(p1).teacherClassSubject(tcs).build();
        Assessment a2 = Assessment.builder().id(2).period(p2).teacherClassSubject(tcs).build();

        // Meta 18.0. Notas nos fechados: 3.0 + 3.0 = 6.0. Resta 1 período aberto -> max possible = 6 + 10 = 16.0 < 18.0 -> FAILED
        Grade g1 = Grade.builder().id(1).student(student).assessment(a1).value(BigDecimal.valueOf(3.0)).build();
        Grade g2 = Grade.builder().id(2).student(student).assessment(a2).value(BigDecimal.valueOf(3.0)).build();

        when(gradeRepository.findByStudentsAndYear(anyList(), eq(year)))
                .thenReturn(List.of(g1, g2));

        List<ReportCardDTO> cards = reportCardService.generateReportCardsBatch(List.of(enrollment), year);

        ReportCardDTO.SubjectSituationDTO subj = cards.get(0).subjects().get(0);
        assertEquals(StudentSituation.FAILED, subj.situation());
        assertEquals(StudentSituation.FAILED, cards.get(0).generalSituation());
    }

    @Test
    @DisplayName("Quando restam períodos abertos mas o aluno já atingiu a meta nos períodos fechados, situação deve ser APPROVED")
    void testRemainingPeriodsOpenAlreadyReachedTargetMustBeApproved() {
        AcademicPeriod p1 = AcademicPeriod.builder().id(1).name("1º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p2 = AcademicPeriod.builder().id(2).name("2º Tri").closed(true).periodType(AcademicPeriodType.TRIMESTER).build();
        AcademicPeriod p3 = AcademicPeriod.builder().id(3).name("3º Tri").closed(false).periodType(AcademicPeriodType.TRIMESTER).build();

        when(academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.TRIMESTER))
                .thenReturn(List.of(p1, p2, p3));

        Assessment a1 = Assessment.builder().id(1).period(p1).teacherClassSubject(tcs).build();
        Assessment a2 = Assessment.builder().id(2).period(p2).teacherClassSubject(tcs).build();

        // Meta 18.0. Notas nos fechados: 9.5 + 9.5 = 19.0 >= 18.0 -> APPROVED
        Grade g1 = Grade.builder().id(1).student(student).assessment(a1).value(BigDecimal.valueOf(9.5)).build();
        Grade g2 = Grade.builder().id(2).student(student).assessment(a2).value(BigDecimal.valueOf(9.5)).build();

        when(gradeRepository.findByStudentsAndYear(anyList(), eq(year)))
                .thenReturn(List.of(g1, g2));

        List<ReportCardDTO> cards = reportCardService.generateReportCardsBatch(List.of(enrollment), year);

        ReportCardDTO.SubjectSituationDTO subj = cards.get(0).subjects().get(0);
        assertEquals(StudentSituation.APPROVED, subj.situation());
        assertEquals(StudentSituation.APPROVED, cards.get(0).generalSituation());
    }
}
