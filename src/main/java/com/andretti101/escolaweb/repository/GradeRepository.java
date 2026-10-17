package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Integer> {

    @Override
    @EntityGraph(attributePaths = {
        "student", "assessment", "assessment.period", "assessment.period.academicYear", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject", "assessment.teacherClassSubject.teacher", "assessment.teacherClassSubject.classRoom"
    })
    Optional<Grade> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {
        "student", "assessment", "assessment.period", "assessment.period.academicYear", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject", "assessment.teacherClassSubject.teacher", "assessment.teacherClassSubject.classRoom"
    })
    List<Grade> findAll();

    @Override
    @EntityGraph(attributePaths = {
        "student", "assessment", "assessment.period", "assessment.period.academicYear", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject", "assessment.teacherClassSubject.teacher", "assessment.teacherClassSubject.classRoom"
    })
    Page<Grade> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
        "student", "assessment", "assessment.period", "assessment.period.academicYear", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject", "assessment.teacherClassSubject.teacher", "assessment.teacherClassSubject.classRoom"
    })
    List<Grade> findByStudent(Student student);

    @EntityGraph(attributePaths = {
        "student", "assessment", "assessment.period", "assessment.period.academicYear", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject", "assessment.teacherClassSubject.teacher", "assessment.teacherClassSubject.classRoom"
    })
    List<Grade> findByAssessment(Assessment assessment);

    @EntityGraph(attributePaths = {"student", "assessment", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject"})
    List<Grade> findByStudentAndAssessment_TeacherClassSubject(com.andretti101.escolaweb.model.entity.Student student, com.andretti101.escolaweb.model.entity.TeacherClassSubject tcs);
    
    @EntityGraph(attributePaths = {"student", "assessment", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject"})
    List<Grade> findByStudent_Id(Integer studentId);

    @EntityGraph(attributePaths = {"student", "assessment", "assessment.teacherClassSubject", "assessment.teacherClassSubject.subject", "assessment.period"})
    @Query("SELECT g FROM Grade g WHERE g.student IN :students AND g.assessment.period.academicYear = :year")
    List<Grade> findByStudentsAndYear(@Param("students") List<com.andretti101.escolaweb.model.entity.Student> students, @Param("year") AcademicYear year);

    Optional<Grade> findByStudent_IdAndAssessment_Id(Integer studentId, Integer assessmentId);

    long countByStudent_IdAndAssessment_Id(Integer studentId, Integer assessmentId);

    boolean existsByStudent(Student student);
    boolean existsByStudentAndAssessment_TeacherClassSubject_ClassRoom(Student student, com.andretti101.escolaweb.model.entity.ClassRoom classRoom);
    boolean existsByStudentAndAssessment(Student student, Assessment assessment);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Grade g SET g.active = false WHERE g.assessment.period.academicYear = :year AND g.active = true")
    int deactivateAllByAcademicYear(@Param("year") AcademicYear academicYear);

    @Query("SELECT g FROM Grade g JOIN FETCH g.assessment a JOIN FETCH a.teacherClassSubject tcs JOIN FETCH tcs.teacher t WHERE g.id = :id")
    Optional<Grade> findWithTcsById(@Param("id") Integer id);
}
