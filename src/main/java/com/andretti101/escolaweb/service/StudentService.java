package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Student;

import java.util.List;

public interface StudentService {
    Student create(Student student);
    Student update(Integer id, Student student);
    void delete(Integer id);
    Student findById(Integer id);
    List<Student> findAll();
    List<Student> findAllActive();
    Student activate(Integer id);
    Student deactivate(Integer id);
}
