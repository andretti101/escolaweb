package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
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
        AcademicPeriodType periodType,
        String periodTypeLabel,
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
            StudentSituation situation,
            BigDecimal finalAverage
    ) {
        public SubjectSituationDTO(
                Integer tcsId,
                String subjectName,
                List<PeriodGradeDTO> periodGrades,
                BigDecimal totalScore,
                BigDecimal targetScore,
                BigDecimal maxPossibleScore,
                StudentSituation situation
        ) {
            this(tcsId, subjectName, periodGrades, totalScore, targetScore, maxPossibleScore, situation,
                    computeDefaultFinalAverage(periodGrades));
        }

        private static BigDecimal computeDefaultFinalAverage(List<PeriodGradeDTO> periodGrades) {
            if (periodGrades == null || periodGrades.isEmpty()) {
                return null;
            }
            List<BigDecimal> graded = periodGrades.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(PeriodGradeDTO::average)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (graded.isEmpty()) {
                return null;
            }
            BigDecimal sum = graded.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            return sum.divide(BigDecimal.valueOf(graded.size()), 2, java.math.RoundingMode.HALF_UP);
        }
    }

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
