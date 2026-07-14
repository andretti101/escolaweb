package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.TeacherClassSubject;

import java.util.List;

public interface TeacherClassSubjectService {
    TeacherClassSubject create(TeacherClassSubject teacherClassSubject);
    TeacherClassSubject update(Integer id, TeacherClassSubject teacherClassSubject);
    void delete(Integer id);
    TeacherClassSubject findById(Integer id);
    List<TeacherClassSubject> findAll();
    List<TeacherClassSubject> findByTeacher(Integer teacherId);
    List<TeacherClassSubject> findByClassRoom(Integer classRoomId);
}
