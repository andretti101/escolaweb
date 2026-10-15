package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.TeacherPatchRequestDTO;
import com.andretti101.escolaweb.model.entity.Teacher;

import java.util.List;

public interface TeacherService {
    Teacher create(Teacher teacher);
    Teacher update(Integer id, Teacher teacher);
    Teacher patch(Integer id, TeacherPatchRequestDTO dto);
    void delete(Integer id);
    Teacher findById(Integer id);
    List<Teacher> findAll();
    org.springframework.data.domain.Page<Teacher> findAll(org.springframework.data.domain.Pageable pageable);
    List<Teacher> findAllActive();
    org.springframework.data.domain.Page<Teacher> findAllActive(org.springframework.data.domain.Pageable pageable);
    Teacher activate(Integer id);
    Teacher deactivate(Integer id);
}
