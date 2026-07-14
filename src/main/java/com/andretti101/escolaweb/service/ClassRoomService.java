package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.ClassRoom;

import java.util.List;

public interface ClassRoomService {
    ClassRoom create(ClassRoom classRoom);
    ClassRoom update(Integer id, ClassRoom classRoom);
    void delete(Integer id);
    ClassRoom findById(Integer id);
    List<ClassRoom> findAll();
    List<ClassRoom> findAllActive();
    List<ClassRoom> findByAcademicYear(Integer academicYearId);
    ClassRoom activate(Integer id);
    ClassRoom deactivate(Integer id);
}
