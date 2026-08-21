package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.exception.UnauthorizedOperationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {

    private final UserRepository userRepository;

    // ── Entity resolution

    @Override
    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        String email = getAuthenticatedEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário autenticado não encontrado com o e-mail: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public Teacher getAuthenticatedTeacher() {
        User user = getAuthenticatedUser();
        if (user instanceof Teacher teacher) {
            return teacher;
        }
        throw new UnauthorizedOperationException("O usuário autenticado não é um professor.");
    }

    @Override
    @Transactional(readOnly = true)
    public Student getAuthenticatedStudent() {
        User user = getAuthenticatedUser();
        if (user instanceof Student student) {
            return student;
        }
        throw new UnauthorizedOperationException("O usuário autenticado não é um aluno.");
    }

    // ── Role checks

    @Override
    public boolean isTeacher() {
        return hasAuthority("ROLE_TEACHER");
    }

    @Override
    public boolean isStudent() {
        return hasAuthority("ROLE_STUDENT");
    }

    // ── Ownership enforcement

    @Override
    public void enforceTeacherOwnership(TeacherClassSubject tcs) {
        if (isTeacher()) {
            Teacher teacher = getAuthenticatedTeacher();
            if (!tcs.getTeacher().getId().equals(teacher.getId())) {
                throw new UnauthorizedOperationException(
                        "Acesso negado: você não tem permissão para acessar este recurso.");
            }
        }
    }

    @Override
    public void enforceStudentOwnership(Integer studentId) {
        if (isStudent()) {
            Student student = getAuthenticatedStudent();
            if (!student.getId().equals(studentId)) {
                throw new UnauthorizedOperationException(
                        "Acesso negado: você só pode acessar suas próprias informações.");
            }
        }
    }

    // ── Private helpers

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new UnauthorizedOperationException("Nenhum usuário autenticado encontrado.");
        }
        return auth.getName();
    }

    private boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(authority));
    }
}
