package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;

import java.util.List;

public interface AcademicPeriodService {
    AcademicPeriod create(AcademicPeriod period);
    AcademicPeriod update(Integer id, AcademicPeriod period);
    void delete(Integer id);
    AcademicPeriod findById(Integer id);
    List<AcademicPeriod> findAll();
    List<AcademicPeriod> findByAcademicYear(Integer academicYearId);
    AcademicPeriod close(Integer id);
}
