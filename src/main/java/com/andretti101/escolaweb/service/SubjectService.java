package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.Subject;

import java.util.List;

public interface SubjectService {
    Subject create(Subject subject);
    Subject update(Integer id, Subject subject);
    void delete(Integer id);
    Subject findById(Integer id);
    List<Subject> findAll();
    List<Subject> findAllActive();
    Subject activate(Integer id);
    Subject deactivate(Integer id);
}
