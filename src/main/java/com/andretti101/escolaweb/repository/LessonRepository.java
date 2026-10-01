package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    boolean existsByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject"})
    @Query("SELECT l FROM Lesson l WHERE l.teacherClassSubject.classRoom.academicYear = :year")
    List<Lesson> findByAcademicYear(@Param("year") AcademicYear year);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Lesson l SET l.active = false WHERE l.teacherClassSubject.classRoom.academicYear = :year AND l.active = true")
    int deactivateAllByAcademicYear(@org.springframework.data.repository.query.Param("year") AcademicYear academicYear);
}
