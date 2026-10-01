package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"student", "assessment", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject"})
    List<Grade> findByStudentAndAssessment_TeacherClassSubject(com.andretti101.escolaweb.model.entity.Student student, com.andretti101.escolaweb.model.entity.TeacherClassSubject tcs);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"student", "assessment", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject"})
    List<Grade> findByStudent_Id(Integer studentId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"student", "assessment", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject", "assessment.period"})
    @Query("SELECT g FROM Grade g WHERE g.student IN :students AND g.assessment.period.academicYear = :year")
    List<Grade> findByStudentsAndYear(@Param("students") List<com.andretti101.escolaweb.model.entity.Student> students, @Param("year") AcademicYear year);

    Optional<Grade> findByStudent_IdAndAssessment_Id(Integer studentId, Integer assessmentId);

    long countByStudent_IdAndAssessment_Id(Integer studentId, Integer assessmentId);

    boolean existsByStudent(Student student);
    boolean existsByStudentAndAssessment(Student student, Assessment assessment);
    List<Grade> findByStudent(Student student);
    List<Grade> findByAssessment(Assessment assessment);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Grade g SET g.active = false WHERE g.assessment.period.academicYear = :year AND g.active = true")
    int deactivateAllByAcademicYear(@org.springframework.data.repository.query.Param("year") AcademicYear academicYear);
}
