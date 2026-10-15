package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.request.YearConclusionRequestDTO;
import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.*;
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
    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReportCardService reportCardService;
    private final SchoolSettingsService schoolSettingsService;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatReadReceiptRepository chatReadReceiptRepository;
    private final AnnouncementRepository announcementRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final AssessmentRepository assessmentRepository;
    private final LessonRepository lessonRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean canConcludeYear() {
        try {
            AcademicYear activeYear = academicYearService.findActive();
            List<AcademicPeriodType> inUse = classRoomRepository.findDistinctActivePeriodTypesByYear(activeYear);
            return !inUse.isEmpty() && inUse.stream()
                    .allMatch(t -> academicPeriodRepository.areAllPeriodsClosedByType(activeYear, t));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public Map<String, Object> concludeYear(YearConclusionRequestDTO request) {

        AcademicYear activeYear = academicYearService.findActive();

        List<AcademicPeriodType> inUse = classRoomRepository.findDistinctActivePeriodTypesByYear(activeYear);
        if (inUse.isEmpty()) {
            throw new IllegalStateException(
                    "Não é possível concluir o ano letivo " + activeYear.getYear()
                    + ": não há turmas ativas cadastradas.");
        }

        List<String> pending = inUse.stream()
                .filter(t -> !academicPeriodRepository.areAllPeriodsClosedByType(activeYear, t))
                .map(AcademicPeriodType::getLabel)
                .toList();

        if (!pending.isEmpty()) {
            throw new IllegalStateException(
                    "Não é possível concluir o ano letivo " + activeYear.getYear()
                    + ". Feche os períodos das divisões: " + String.join(", ", pending) + ".");
        }

        List<Enrollment> activeEnrollments = enrollmentRepository
                .findByClassRoom_AcademicYearAndActiveTrue(activeYear);

        int approvedCount = 0;
        int failedCount = 0;

        List<ReportCardDTO> batchReportCards = reportCardService.generateReportCardsBatch(activeEnrollments, activeYear);
        for (Enrollment enrollment : activeEnrollments) {
            ReportCardDTO reportCard = batchReportCards.stream()
                    .filter(rc -> rc.studentId().equals(enrollment.getStudent().getId()))
                    .findFirst().orElse(null);

            StudentSituation finalSituation = (reportCard != null && reportCard.generalSituation() == StudentSituation.APPROVED)
                    ? StudentSituation.APPROVED
                    : StudentSituation.FAILED;

            enrollment.setGeneralSituation(finalSituation);

            if (finalSituation == StudentSituation.APPROVED) {
                approvedCount++;
            } else {
                failedCount++;
            }
        }

        enrollmentRepository.saveAll(activeEnrollments);

        // Blindagem defensiva: garantir que nenhuma matrícula do ano concluído permaneça com situação PENDING
        List<Enrollment> allYearEnrollments = enrollmentRepository.findByClassRoom_AcademicYear(activeYear);
        if (allYearEnrollments != null && !allYearEnrollments.isEmpty()) {
            List<Enrollment> pendingLeft = allYearEnrollments.stream()
                    .filter(e -> e.getGeneralSituation() == StudentSituation.PENDING)
                    .toList();
            if (!pendingLeft.isEmpty()) {
                pendingLeft.forEach(e -> e.setGeneralSituation(StudentSituation.FAILED));
                enrollmentRepository.saveAll(pendingLeft);
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

        settings.setMinimumGrade(request.minimumGrade());
        settings.setMinimumAttendance(request.minimumAttendance());

        AcademicYear newYear = AcademicYear.builder()
                .year(request.nextYear())
                .active(true)
                .build();
        AcademicYear savedNewYear = academicYearService.create(newYear);
        savedNewYear.setActive(true);

        Map<String, Object> summary = new HashMap<>();
        summary.put("concludedYear", activeYear.getYear());
        summary.put("newYear", savedNewYear.getYear());
        summary.put("approvedStudents", approvedCount);
        summary.put("failedStudents", failedCount);
        summary.put("totalStudentsProcessed", approvedCount + failedCount);
        summary.put("archivedChatMessages", archivedMessages);
        summary.put("archivedAnnouncements", archivedAnnouncements);
        summary.put("newMinimumGrade", request.minimumGrade());
        summary.put("newMinimumAttendance", request.minimumAttendance());

        return summary;
    }
}
