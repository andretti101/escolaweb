package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.ReportCardDTO;

public interface ReportCardService {
    ReportCardDTO generateReportCard(Integer studentId, Integer academicYearId);
    java.util.List<ReportCardDTO> generateReportCardsBatch(java.util.List<com.andretti101.escolaweb.model.entity.Enrollment> enrollments, com.andretti101.escolaweb.model.entity.AcademicYear year);
}
