package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.StudentPatchRequestDTO;
import com.andretti101.escolaweb.model.entity.Student;

import java.util.List;

public interface StudentService {
    Student create(Student student);
    Student update(Integer id, Student student);
    Student patch(Integer id, StudentPatchRequestDTO dto);
    void delete(Integer id);
    Student findById(Integer id);
    List<Student> findAll();
    org.springframework.data.domain.Page<Student> findAll(org.springframework.data.domain.Pageable pageable);
    List<Student> findAllActive();
    org.springframework.data.domain.Page<Student> findAllActive(org.springframework.data.domain.Pageable pageable);
    List<Student> findAllActiveUnenrolled();
    org.springframework.data.domain.Page<Student> findAllActiveUnenrolled(org.springframework.data.domain.Pageable pageable);
    Student activate(Integer id);
    Student deactivate(Integer id);
}
