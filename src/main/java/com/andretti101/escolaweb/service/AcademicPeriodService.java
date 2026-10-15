package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;

import java.util.List;
import java.util.Map;

public interface AcademicPeriodService {
    AcademicPeriod create(AcademicPeriod period);
    List<AcademicPeriod> createDivision(Integer academicYearId, AcademicPeriodType type);
    AcademicPeriod update(Integer id, AcademicPeriod period);
    void delete(Integer id);
    void deleteDivision(Integer academicYearId, AcademicPeriodType type);
    AcademicPeriod findById(Integer id);
    List<AcademicPeriod> findAll();
    List<AcademicPeriod> findByAcademicYear(Integer academicYearId);
    List<AcademicPeriod> findByAcademicYearAndType(Integer academicYearId, AcademicPeriodType type);
    Map<AcademicPeriodType, List<AcademicPeriod>> findDivisions(Integer academicYearId);
    AcademicPeriod close(Integer id);
    AcademicPeriod openNextPeriod(Integer academicYearId, AcademicPeriodType type);
}
