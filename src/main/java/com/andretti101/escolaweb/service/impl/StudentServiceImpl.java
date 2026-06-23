package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.exception.BusinessException;
import com.andretti101.escolaweb.exception.DuplicateResourceException;
import com.andretti101.escolaweb.exception.ResourceNotFoundException;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.model.enums.UserType;
import com.andretti101.escolaweb.repository.StudentRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Student create(Student student) {
        User user = userRepository.findById(student.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário vinculado não encontrado."));

        if (user.getType() != UserType.STUDENT) {
            throw new BusinessException("Só é possível vincular um aluno a um usuário do tipo ALUNO.");
        }

        if (studentRepository.existsByEnrollment(student.getEnrollment())) {
            throw new DuplicateResourceException("Já existe um aluno cadastrado com esta matrícula.");
        }

        student.setUser(user);
        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public Student update(Integer id, Student student) {
        Student existing = findById(id);

        studentRepository.findByEnrollment(student.getEnrollment()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new DuplicateResourceException("Já existe um aluno cadastrado com esta matrícula.");
            }
        });

        existing.setEnrollment(student.getEnrollment());
        existing.setBirthDate(student.getBirthDate());
        existing.setClassRoom(student.getClassRoom());

        return studentRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Student findById(Integer id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Student student = findById(id);
        studentRepository.delete(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Student findOwnProfile(Integer studentId, User requestingUser) {
        Student student = findById(studentId);

        boolean isOwner = requestingUser.getType() == UserType.STUDENT
                && requestingUser.getId().equals(student.getUser().getId());
        boolean isStaff = requestingUser.getType() == UserType.ADMINISTRATION
                || requestingUser.getType() == UserType.MANAGEMENT
                || requestingUser.getType() == UserType.TEACHER;

        if (!isOwner && !isStaff) {
            throw new AccessDeniedException("Você só pode visualizar suas próprias informações acadêmicas.");
        }

        return student;
    }
}
