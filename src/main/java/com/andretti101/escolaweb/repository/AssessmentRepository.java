package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {
    List<Assessment> findByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    List<Assessment> findByPeriod(AcademicPeriod period);
    boolean existsByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    boolean existsByPeriod(AcademicPeriod period);
    long countByTeacherClassSubjectAndPeriod(TeacherClassSubject teacherClassSubject, AcademicPeriod period);

    @Query("SELECT a FROM Assessment a WHERE a.teacherClassSubject.teacher.id = :teacherId AND a.teacherClassSubject.classRoom.academicYear.active = true")
    Page<Assessment> findByTeacherActiveAcademicYear(@Param("teacherId") Integer teacherId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Assessment a SET a.active = false WHERE a.teacherClassSubject.classRoom.academicYear = :year AND a.active = true")
    int deactivateAllByAcademicYear(@Param("year") com.andretti101.escolaweb.model.entity.AcademicYear academicYear);
}