package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.repository.StudentRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Student create(Student student) {
        validateEmailAvailable(student.getEmail());
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        student.setActive(true);
        return student;
    }

    @Override
    @Transactional
    public Student update(Integer id, Student incoming) {
        Student existing = findStudentOrThrow(id);

        if (!existing.getEmail().equals(incoming.getEmail())) {
            validateEmailNotTakenByAnother(incoming.getEmail(), id);
        }

        existing.setName(incoming.getName());
        existing.setEmail(incoming.getEmail());
        existing.setOwnPhone(incoming.getOwnPhone());
        existing.setGuardianPhone(incoming.getGuardianPhone());
        existing.setBirthDate(incoming.getBirthDate());

        return studentRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        findStudentOrThrow(id);
        studentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Student findById(Integer id) {
        return findStudentOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findAllActive() {
        return studentRepository.findByActiveTrue();
    }

    @Override
    @Transactional
    public Student activate(Integer id) {
        Student student = findStudentOrThrow(id);
        student.setActive(true);
        return student;
    }

    @Override
    @Transactional
    public Student deactivate(Integer id) {
        Student student = findStudentOrThrow(id);
        student.setActive(false);
        return student;
    }

    Student findStudentOrThrow(Integer id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + id));
    }

    private void validateEmailAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already in use: " + email);
        }
    }

    private void validateEmailNotTakenByAnother(String email, Integer currentId) {
        if (userRepository.existsByEmailAndIdNot(email, currentId)) {
            throw new IllegalStateException("Email already in use: " + email);
        }
    }
}
