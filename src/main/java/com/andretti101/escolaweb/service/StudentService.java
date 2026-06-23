package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.User;

import java.util.List;

public interface StudentService {

    Student create(Student student);

    Student update(Integer id, Student student);

    Student findById(Integer id);

    List<Student> findAll();

    void delete(Integer id);

    Student findOwnProfile(Integer studentId, User requestingUser);
}