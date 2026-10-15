package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;

import java.time.LocalDateTime;
import java.util.List;

public record ClassRoomResponseDTO(
        Integer id,
        String name,
        Shift shift,
        SchoolGrade schoolGrade,
        String schoolGradeLabel,
        AcademicPeriodType periodType,
        String periodTypeLabel,
        Integer academicYearId,
        Integer academicYear,
        boolean active,
        LocalDateTime createdAt,
        List<AcademicPeriodResponseDTO> periods
) {
    public ClassRoomResponseDTO(
            Integer id,
            String name,
            Shift shift,
            SchoolGrade schoolGrade,
            AcademicPeriodType periodType,
            String periodTypeLabel,
            Integer academicYearId,
            Integer academicYear,
            boolean active,
            LocalDateTime createdAt,
            List<AcademicPeriodResponseDTO> periods
    ) {
        this(id, name, shift, schoolGrade,
                schoolGrade != null ? schoolGrade.getLabel() : null,
                periodType, periodTypeLabel, academicYearId, academicYear, active, createdAt, periods);
    }

    public ClassRoomResponseDTO(
            Integer id,
            String name,
            Shift shift,
            SchoolGrade schoolGrade,
            Integer academicYearId,
            Integer academicYear,
            boolean active,
            LocalDateTime createdAt
    ) {
        this(id, name, shift, schoolGrade,
                schoolGrade != null ? schoolGrade.getLabel() : null,
                null, null, academicYearId, academicYear, active, createdAt, List.of());
    }
}
