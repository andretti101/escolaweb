package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Enrollment;

import java.util.List;

public interface EnrollmentService {
    Enrollment create(Enrollment enrollment);
    Enrollment update(Integer id, Enrollment enrollment);
    void delete(Integer id);
    Enrollment findById(Integer id);
    List<Enrollment> findAll();
    org.springframework.data.domain.Page<Enrollment> findAll(org.springframework.data.domain.Pageable pageable);
    List<Enrollment> findAllActive();
    org.springframework.data.domain.Page<Enrollment> findAllActive(org.springframework.data.domain.Pageable pageable);
    List<Enrollment> findByStudent(Integer studentId);
    List<Enrollment> findByClassRoom(Integer classRoomId);
    Enrollment activate(Integer id);
    Enrollment deactivate(Integer id);
    Enrollment transferClassroom(Integer enrollmentId, Integer newClassRoomId);
}
