package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.StudentSituation;

import java.math.BigDecimal;
import java.util.List;

public record ReportCardDTO(
        Integer studentId,
        String studentName,
        String registrationNumber,
        Integer academicYearId,
        Integer academicYear,
        String classRoomName,
        BigDecimal minimumGrade,
        int totalPeriods,
        BigDecimal targetScore,
        List<SubjectSituationDTO> subjects,
        FrequencySituationDTO frequency,
        StudentSituation generalSituation
) {

    public record SubjectSituationDTO(
            Integer tcsId,
            String subjectName,
            List<PeriodGradeDTO> periodGrades,
            BigDecimal totalScore,
            BigDecimal targetScore,
            BigDecimal maxPossibleScore,
            StudentSituation situation
    ) {}

    public record PeriodGradeDTO(
            Integer periodId,
            String periodName,
            boolean closed,
            BigDecimal average
    ) {}

    public record FrequencySituationDTO(
            BigDecimal generalFrequency,
            BigDecimal minimumAttendance,
            StudentSituation situation
    ) {}
}
