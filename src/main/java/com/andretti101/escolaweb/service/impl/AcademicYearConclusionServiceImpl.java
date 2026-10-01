package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.request.YearConclusionRequestDTO;
import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AnnouncementRepository;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.repository.ChatMessageRepository;
import com.andretti101.escolaweb.repository.ChatReadReceiptRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AcademicYearConclusionService;
import com.andretti101.escolaweb.service.AcademicYearService;
import com.andretti101.escolaweb.service.ReportCardService;
import com.andretti101.escolaweb.service.SchoolSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AcademicYearConclusionServiceImpl implements AcademicYearConclusionService {

    private final AcademicYearService academicYearService;
    private final AcademicPeriodService academicPeriodService;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReportCardService reportCardService;
    private final SchoolSettingsService schoolSettingsService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadReceiptRepository chatReadReceiptRepository;
    private final AnnouncementRepository announcementRepository;
    private final ClassRoomRepository classRoomRepository;
    private final com.andretti101.escolaweb.repository.TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final com.andretti101.escolaweb.repository.AssessmentRepository assessmentRepository;
    private final com.andretti101.escolaweb.repository.LessonRepository lessonRepository;
    private final com.andretti101.escolaweb.repository.GradeRepository gradeRepository;
    private final com.andretti101.escolaweb.repository.AttendanceRepository attendanceRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean canConcludeYear() {
        try {
            AcademicYear activeYear = academicYearService.findActive();
            return academicPeriodRepository.areAllPeriodsClosed(activeYear);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> concludeYear(YearConclusionRequestDTO request) {

        AcademicYear activeYear = academicYearService.findActive();

        if (!academicPeriodRepository.areAllPeriodsClosed(activeYear)) {
            throw new IllegalStateException(
                    "Não é possível concluir o ano letivo " + activeYear.getYear()
                    + ". Todos os períodos acadêmicos devem estar fechados antes da conclusão.");
        }

        List<Enrollment> activeEnrollments = enrollmentRepository
                .findByClassRoom_AcademicYearAndActiveTrue(activeYear);

        int approvedCount = 0;
        int failedCount = 0;

        List<com.andretti101.escolaweb.dto.response.ReportCardDTO> batchReportCards = reportCardService.generateReportCardsBatch(activeEnrollments, activeYear);
        for (Enrollment enrollment : activeEnrollments) {
            com.andretti101.escolaweb.dto.response.ReportCardDTO reportCard = batchReportCards.stream()
                    .filter(rc -> rc.studentId().equals(enrollment.getStudent().getId()))
                    .findFirst().orElse(null);

            if (reportCard != null) {
                StudentSituation generalSituation = reportCard.generalSituation();
                enrollment.setGeneralSituation(generalSituation);

                if (generalSituation == StudentSituation.APPROVED) {
                    approvedCount++;
                } else {
                    failedCount++;
                }
            }
        }

        enrollmentRepository.deactivateAllByAcademicYear(activeYear);
        assessmentRepository.deactivateAllByAcademicYear(activeYear);
        lessonRepository.deactivateAllByAcademicYear(activeYear);
        teacherClassSubjectRepository.deactivateAllByAcademicYear(activeYear);
        gradeRepository.deactivateAllByAcademicYear(activeYear);
        attendanceRepository.deactivateAllByAcademicYear(activeYear);
        classRoomRepository.deactivateAllByAcademicYear(activeYear);

        int archivedMessages = chatMessageRepository.archiveAll();
        chatReadReceiptRepository.deleteAllNonGlobal();
        int archivedAnnouncements = announcementRepository.archiveAll();

        activeYear.setActive(false);

        SchoolSettings settings = schoolSettingsService.findSettings();
        
        activeYear.setMinimumGrade(settings.getMinimumGrade());
        activeYear.setMinimumAttendance(settings.getMinimumAttendance());
        activeYear.setPeriodType(settings.getPeriodType());

        settings.setMinimumGrade(request.minimumGrade());
        settings.setMinimumAttendance(request.minimumAttendance());
        settings.setPeriodType(request.periodType());

        AcademicYear newYear = AcademicYear.builder()
                .year(request.nextYear())
                .active(true)
                .build();
        AcademicYear savedNewYear = academicYearService.create(newYear);
        savedNewYear.setActive(true);

        academicPeriodService.generatePeriodsForYear(savedNewYear.getId());

        Map<String, Object> summary = new HashMap<>();
        summary.put("concludedYear", activeYear.getYear());
        summary.put("newYear", savedNewYear.getYear());
        summary.put("approvedStudents", approvedCount);
        summary.put("failedStudents", failedCount);
        summary.put("totalStudentsProcessed", approvedCount + failedCount);
        summary.put("archivedChatMessages", archivedMessages);
        summary.put("archivedAnnouncements", archivedAnnouncements);
        summary.put("newPeriodType", request.periodType().getLabel());
        summary.put("newMinimumGrade", request.minimumGrade());
        summary.put("newMinimumAttendance", request.minimumAttendance());

        return summary;
    }
}
