package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Attendance;

import java.math.BigDecimal;
import java.util.List;

public interface AttendanceService {
    Attendance register(Attendance attendance);
    Attendance update(Integer id, Attendance attendance);
    void delete(Integer id);
    Attendance findById(Integer id);
    List<Attendance> findAll();
    List<Attendance> findByLesson(Integer lessonId);
    List<Attendance> findByStudent(Integer studentId);
    BigDecimal calculateFrequency(Integer studentId, Integer teacherClassSubjectId);
}
