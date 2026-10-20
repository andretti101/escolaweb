package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Grade;

import java.math.BigDecimal;
import java.util.List;

public interface GradeService {
    Grade launch(Grade grade);
    Grade launch(Grade grade, String notes);
    Grade update(Integer id, Grade grade);
    Grade update(Integer id, Grade grade, String notes);
    Grade launchRecovery(Integer id, BigDecimal recoveryValue, String notes);
    Grade removeRecovery(Integer id, String notes);
    void delete(Integer id);
    Grade findById(Integer id);
    List<Grade> findAll();
    org.springframework.data.domain.Page<Grade> findAll(org.springframework.data.domain.Pageable pageable);
    List<Grade> findByStudent(Integer studentId);
    List<Grade> findByAssessment(Integer assessmentId);
    BigDecimal calculateAverage(Integer studentId, Integer teacherClassSubjectId);
}
