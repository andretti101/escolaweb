package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Assessment;

import java.util.List;

public interface AssessmentService {
    Assessment create(Assessment assessment);
    Assessment update(Integer id, Assessment assessment);
    void delete(Integer id);
    Assessment findById(Integer id);
    List<Assessment> findAll();
    org.springframework.data.domain.Page<Assessment> findAll(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Assessment> findByTeacherActiveAcademicYear(Integer teacherId, org.springframework.data.domain.Pageable pageable);
    List<Assessment> findByTeacherClassSubject(Integer teacherClassSubjectId);
    List<Assessment> findByPeriod(Integer periodId);
}
