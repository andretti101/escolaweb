package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.exception.BusinessException;
import com.andretti101.escolaweb.exception.ResourceNotFoundException;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.model.enums.UserType;
import com.andretti101.escolaweb.repository.TeacherRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Teacher create(Teacher teacher) {
        User user = userRepository.findById(teacher.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário vinculado não encontrado."));

        if (user.getType() != UserType.STUDENT) {
            throw new BusinessException("Só é possível vincular um professor a um usuário do tipo PROFESSOR.");
        }

        teacher.setUser(user);
        return teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public Teacher update(Integer id, Teacher teacher) {
        return findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Teacher findById(Integer id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> findAll() {
        return teacherRepository.findAll();
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Teacher teacher = findById(id);
        teacherRepository.delete(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherClassSubject> findClassSubjects(Integer teacherId) {
        Teacher teacher = findById(teacherId);
        return teacher.getTeacherClassSubjects();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean teachesClassSubject(Integer teacherId, Integer classSubjectId) {
        return findClassSubjects(teacherId).stream()
                .anyMatch(tcs -> Objects.equals(tcs.getId(), classSubjectId));
    }

    @Override
    public void validateOwnership(Integer teacherId, Integer classSubjectId) {
        if (!teachesClassSubject(teacherId, classSubjectId)) {
            throw new AccessDeniedException("Este professor não leciona a disciplina/turma informada.");
        }
    }
}
