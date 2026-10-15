package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.ClassRoom;

import java.util.List;

public interface ClassRoomService {
    ClassRoom create(ClassRoom classRoom);
    ClassRoom update(Integer id, ClassRoom classRoom);
    void delete(Integer id);
    ClassRoom findById(Integer id);
    List<ClassRoom> findAll();
    org.springframework.data.domain.Page<ClassRoom> findAll(org.springframework.data.domain.Pageable pageable);
    List<ClassRoom> findAllActive();
    org.springframework.data.domain.Page<ClassRoom> findAllActive(org.springframework.data.domain.Pageable pageable);
    List<ClassRoom> findByAcademicYear(Integer academicYearId);
    org.springframework.data.domain.Page<ClassRoom> findByAcademicYear(Integer academicYearId, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<ClassRoom> findByAcademicYearAndActiveTrue(Integer academicYearId, org.springframework.data.domain.Pageable pageable);
    ClassRoom activate(Integer id);
    ClassRoom deactivate(Integer id);
}
