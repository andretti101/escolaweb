package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.repository.TeacherRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.TeacherService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Teacher create(Teacher teacher) {
        if (userRepository.existsByEmail(teacher.getEmail())) {
            throw new IllegalStateException("Email already in use: " + teacher.getEmail());
        }
        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));
        teacher.setActive(true);
        return teacher;
    }

    @Override
    @Transactional
    public Teacher update(Integer id, Teacher incoming) {
        Teacher existing = findTeacherOrThrow(id);

        if (!existing.getEmail().equals(incoming.getEmail())
                && userRepository.existsByEmailAndIdNot(incoming.getEmail(), id)) {
            throw new IllegalStateException("Email already in use: " + incoming.getEmail());
        }

        existing.setName(incoming.getName());
        existing.setEmail(incoming.getEmail());

        return teacherRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Teacher teacher = findTeacherOrThrow(id);

        if (teacherClassSubjectRepository.existsByTeacher(teacher)) {
            throw new IllegalStateException(
                    "Cannot delete teacher with id " + id + " because they have active class assignments.");
        }

        teacherRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Teacher findById(Integer id) {
        return findTeacherOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findAllActive() {
        return teacherRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Teacher activate(Integer id) {
        Teacher teacher = findTeacherOrThrow(id);
        teacher.setActive(true);
        return teacher;
    }

    @Override
    @Transactional
    public Teacher deactivate(Integer id) {
        Teacher teacher = findTeacherOrThrow(id);
        teacher.setActive(false);
        return teacher;
    }

    Teacher findTeacherOrThrow(Integer id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found with id: " + id));
    }
}
