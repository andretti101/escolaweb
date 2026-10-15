package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.YearConclusionRequestDTO;
import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.impl.AcademicYearConclusionServiceImpl;
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
public class AcademicYearConclusionServiceImplTest {

    @Mock
    private AcademicYearService academicYearService;
    @Mock
    private AcademicPeriodRepository academicPeriodRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ReportCardService reportCardService;
    @Mock
    private SchoolSettingsService schoolSettingsService;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatReadReceiptRepository chatReadReceiptRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private ClassRoomRepository classRoomRepository;
    @Mock
    private TeacherClassSubjectRepository teacherClassSubjectRepository;
    @Mock
    private AssessmentRepository assessmentRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private AttendanceRepository attendanceRepository;

    @InjectMocks
    private AcademicYearConclusionServiceImpl conclusionService;

    @Test
    @DisplayName("Blindagem defensiva: qualquer situação que não seja APPROVED (inclusive PENDING ou nulo) deve ser gravada como FAILED")
    void testDefensiveShieldConcludesNonApprovedAsFailed() {
        AcademicYear activeYear = AcademicYear.builder().id(1).year(2026).active(true).build();
        ClassRoom cr = ClassRoom.builder().id(10).academicYear(activeYear).periodType(AcademicPeriodType.TRIMESTER).build();
        Student s1 = Student.builder().id(101).name("Aluno Aprovado").build();
        Student s2 = Student.builder().id(102).name("Aluno Pendente Que Deve Virar Reprovado").build();
        Student s3 = Student.builder().id(103).name("Aluno Sem Boletim").build();

        Enrollment e1 = Enrollment.builder().id(1).student(s1).classRoom(cr).active(true).generalSituation(StudentSituation.PENDING).build();
        Enrollment e2 = Enrollment.builder().id(2).student(s2).classRoom(cr).active(true).generalSituation(StudentSituation.PENDING).build();
        Enrollment e3 = Enrollment.builder().id(3).student(s3).classRoom(cr).active(true).generalSituation(StudentSituation.PENDING).build();

        when(academicYearService.findActive()).thenReturn(activeYear);
        when(classRoomRepository.findDistinctActivePeriodTypesByYear(activeYear)).thenReturn(List.of(AcademicPeriodType.TRIMESTER));
        when(academicPeriodRepository.areAllPeriodsClosedByType(activeYear, AcademicPeriodType.TRIMESTER)).thenReturn(true);
        when(enrollmentRepository.findByClassRoom_AcademicYearAndActiveTrue(activeYear)).thenReturn(List.of(e1, e2, e3));
        when(enrollmentRepository.findByClassRoom_AcademicYear(activeYear)).thenReturn(List.of(e1, e2, e3));

        ReportCardDTO rc1 = mock(ReportCardDTO.class);
        when(rc1.studentId()).thenReturn(101);
        when(rc1.generalSituation()).thenReturn(StudentSituation.APPROVED);

        ReportCardDTO rc2 = mock(ReportCardDTO.class);
        when(rc2.studentId()).thenReturn(102);
        when(rc2.generalSituation()).thenReturn(StudentSituation.PENDING); // Simulando boletim que de alguma forma veio PENDING

        when(reportCardService.generateReportCardsBatch(anyList(), eq(activeYear))).thenReturn(List.of(rc1, rc2));

        SchoolSettings settings = SchoolSettings.builder()
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .build();
        when(schoolSettingsService.findSettings()).thenReturn(settings);

        AcademicYear newYear = AcademicYear.builder().id(2).year(2027).active(true).build();
        when(academicYearService.create(any(AcademicYear.class))).thenReturn(newYear);

        YearConclusionRequestDTO request = new YearConclusionRequestDTO(2027, BigDecimal.valueOf(6.0), BigDecimal.valueOf(75.0));

        var summary = conclusionService.concludeYear(request);

        assertEquals(1, summary.get("approvedStudents"));
        assertEquals(2, summary.get("failedStudents"));
        assertEquals(3, summary.get("totalStudentsProcessed"));

        assertEquals(StudentSituation.APPROVED, e1.getGeneralSituation());
        assertEquals(StudentSituation.FAILED, e2.getGeneralSituation(), "PENDING deve ter sido convertido defensivamente para FAILED");
        assertEquals(StudentSituation.FAILED, e3.getGeneralSituation(), "Sem boletim deve ser gravado defensivamente como FAILED");
    }
}
