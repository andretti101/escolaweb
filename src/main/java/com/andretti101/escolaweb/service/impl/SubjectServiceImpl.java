package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.repository.SubjectRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.SubjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Override
    @Transactional
    public Subject create(Subject subject) {
        validateNameAvailable(subject.getName(), null);
        subject.setActive(true);
        return subject;
    }

    @Override
    @Transactional
    public Subject update(Integer id, Subject incoming) {
        Subject existing = findSubjectOrThrow(id);

        if (!existing.getName().equalsIgnoreCase(incoming.getName())) {
            validateNameAvailable(incoming.getName(), id);
        }

        existing.setName(incoming.getName());
        existing.setDescription(incoming.getDescription());

        return subjectRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Subject subject = findSubjectOrThrow(id);

        if (teacherClassSubjectRepository.existsBySubject(subject)) {
            throw new IllegalStateException(
                    "Cannot delete subject with id " + id + " because it is linked to class assignments.");
        }

        subjectRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Subject findById(Integer id) {
        return findSubjectOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subject> findAllActive() {
        return subjectRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Subject activate(Integer id) {
        Subject subject = findSubjectOrThrow(id);
        subject.setActive(true);
        return subject;
    }

    @Override
    @Transactional
    public Subject deactivate(Integer id) {
        Subject subject = findSubjectOrThrow(id);
        subject.setActive(false);
        return subject;
    }

    Subject findSubjectOrThrow(Integer id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found with id: " + id));
    }

    private void validateNameAvailable(String name, Integer excludeId) {
        subjectRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw new IllegalStateException("Subject name already in use: " + name);
            }
        });
    }
}
