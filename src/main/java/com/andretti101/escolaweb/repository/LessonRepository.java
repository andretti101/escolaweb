package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
    boolean existsByTeacherClassSubject(TeacherClassSubject teacherClassSubject);
}
