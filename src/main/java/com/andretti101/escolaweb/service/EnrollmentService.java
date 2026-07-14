package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Enrollment;

import java.util.List;

public interface EnrollmentService {
    Enrollment create(Enrollment enrollment);
    Enrollment update(Integer id, Enrollment enrollment);
    void delete(Integer id);
    Enrollment findById(Integer id);
    List<Enrollment> findAll();
    List<Enrollment> findAllActive();
    List<Enrollment> findByStudent(Integer studentId);
    List<Enrollment> findByClassRoom(Integer classRoomId);
    Enrollment activate(Integer id);
    Enrollment deactivate(Integer id);
}
