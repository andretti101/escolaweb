package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {
    List<Assessment> findByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    List<Assessment> findByPeriod(AcademicPeriod period);
    boolean existsByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    boolean existsByPeriod(AcademicPeriod period);
}
