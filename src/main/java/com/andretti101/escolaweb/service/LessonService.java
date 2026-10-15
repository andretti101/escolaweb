package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Lesson;

import java.util.List;

public interface LessonService {
    Lesson create(Lesson lesson);
    Lesson update(Integer id, Lesson lesson);
    void delete(Integer id);
    Lesson findById(Integer id);
    List<Lesson> findAll();
    org.springframework.data.domain.Page<Lesson> findAll(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Lesson> findByTeacherActiveAcademicYear(Integer teacherId, org.springframework.data.domain.Pageable pageable);
    List<Lesson> findByTeacherClassSubject(Integer teacherClassSubjectId);
}
