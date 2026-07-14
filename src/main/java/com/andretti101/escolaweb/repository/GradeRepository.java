package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    List<Grade> findByStudent(Student student);
    List<Grade> findByAssessment(Assessment assessment);
    boolean existsByStudent(Student student);
    boolean existsByStudentAndAssessment(Student student, Assessment assessment);
    boolean existsByAssessment(Assessment assessment);
    List<Grade> findByStudentAndAssessment_TeacherClassSubject(Student student, TeacherClassSubject teacherClassSubject);
}
