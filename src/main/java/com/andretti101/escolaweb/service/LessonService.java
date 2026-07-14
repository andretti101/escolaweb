package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Lesson;

import java.util.List;

public interface LessonService {
    Lesson create(Lesson lesson);
    Lesson update(Integer id, Lesson lesson);
    void delete(Integer id);
    Lesson findById(Integer id);
    List<Lesson> findAll();
    List<Lesson> findByTeacherClassSubject(Integer teacherClassSubjectId);
}
