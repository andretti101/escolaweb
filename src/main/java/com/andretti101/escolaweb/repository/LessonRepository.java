package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.AcademicYear;
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
public interface LessonRepository extends JpaRepository<Lesson, Integer> {

    @Override
    @EntityGraph(attributePaths = {
        "teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "teacherClassSubject.classRoom.academicYear"
    })
    Optional<Lesson> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {
        "teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "teacherClassSubject.classRoom.academicYear"
    })
    List<Lesson> findAll();

    @Override
    @EntityGraph(attributePaths = {
        "teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "teacherClassSubject.classRoom.academicYear"
    })
    Page<Lesson> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
        "teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "teacherClassSubject.classRoom.academicYear"
    })
    List<Lesson> findByTeacherClassSubject(TeacherClassSubject teacherClassSubject);

    @EntityGraph(attributePaths = {
        "teacherClassSubject", "teacherClassSubject.subject", "teacherClassSubject.teacher", "teacherClassSubject.classRoom", "teacherClassSubject.classRoom.academicYear"
    })
    @Query("SELECT l FROM Lesson l WHERE l.teacherClassSubject.teacher.id = :teacherId AND l.teacherClassSubject.classRoom.academicYear.active = true")
    Page<Lesson> findByTeacherActiveAcademicYear(@Param("teacherId") Integer teacherId, Pageable pageable);

    boolean existsByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    
    @EntityGraph(attributePaths = {"teacherClassSubject", "teacherClassSubject.subject"})
    @Query("SELECT l FROM Lesson l WHERE l.teacherClassSubject.classRoom.academicYear = :year")
    List<Lesson> findByAcademicYear(@Param("year") AcademicYear year);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Lesson l SET l.active = false WHERE l.teacherClassSubject.classRoom.academicYear = :year AND l.active = true")
    int deactivateAllByAcademicYear(@Param("year") AcademicYear academicYear);
}
