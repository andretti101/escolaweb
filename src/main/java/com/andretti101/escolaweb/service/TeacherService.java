package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;

import java.util.List;

public interface TeacherService {

    Teacher create(Teacher teacher);

    Teacher update(Integer id, Teacher teacher);

    Teacher findById(Integer id);

    List<Teacher> findAll();

    void delete(Integer id);

    List<TeacherClassSubject> findClassSubjects(Integer teacherId);

    boolean teachesClassSubject(Integer teacherId, Integer classSubjectId);

    void validateOwnership(Integer teacherId, Integer classSubjectId);
}