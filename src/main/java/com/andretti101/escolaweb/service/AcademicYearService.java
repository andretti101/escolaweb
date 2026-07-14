package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.AcademicYear;

import java.util.List;

public interface AcademicYearService {
    AcademicYear create(AcademicYear academicYear);
    AcademicYear update(Integer id, AcademicYear academicYear);
    void delete(Integer id);
    AcademicYear findById(Integer id);
    List<AcademicYear> findAll();
    AcademicYear findActive();
    AcademicYear setActive(Integer id);
}
