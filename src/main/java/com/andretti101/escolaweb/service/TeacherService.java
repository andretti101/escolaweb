package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Teacher;

import java.util.List;

public interface TeacherService {
    Teacher create(Teacher teacher);
    Teacher update(Integer id, Teacher teacher);
    void delete(Integer id);
    Teacher findById(Integer id);
    List<Teacher> findAll();
    List<Teacher> findAllActive();
    Teacher activate(Integer id);
    Teacher deactivate(Integer id);
}
