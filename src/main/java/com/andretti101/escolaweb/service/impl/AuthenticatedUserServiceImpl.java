package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Announcement;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.exception.UnauthorizedOperationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

// Utilitário de segurança para resolução polimórfica da entidade do usuário logado a partir do SecurityContextHolder
@Service
@RequiredArgsConstructor
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;

    // Recupera a entidade completa do usuário conectado com base no principal JWT
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

    @Override
    @Transactional(readOnly = true)
    public void enforceStudentClassRoomOwnership(ClassRoom classRoom) {
        if (isStudent() && classRoom != null && classRoom.getId() != null) {
            Student student = getAuthenticatedStudent();
            boolean enrolled = enrollmentRepository.existsByStudent_IdAndClassRoom_IdAndActiveTrue(
                    student.getId(), classRoom.getId());
            if (!enrolled) {
                throw new UnauthorizedOperationException(
                        "Acesso negado: você não está matriculado na turma deste recurso.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceTeacherStudentAccess(Integer studentId) {
        if (isTeacher() && studentId != null) {
            Teacher teacher = getAuthenticatedTeacher();
            Set<Integer> teacherClassIds = teacherClassSubjectRepository.findByTeacher(teacher).stream()
                    .filter(tcs -> tcs.getClassRoom() != null && tcs.getClassRoom().getId() != null)
                    .map(tcs -> tcs.getClassRoom().getId())
                    .collect(Collectors.toSet());
            Student ref = Student.builder().id(studentId).build();
            var studentEnrollments = enrollmentRepository.findByStudent(ref);
            if (!studentEnrollments.isEmpty()) {
                boolean teachesStudent = studentEnrollments.stream()
                        .anyMatch(e -> e.getClassRoom() != null && teacherClassIds.contains(e.getClassRoom().getId()));
                if (!teachesStudent) {
                    throw new UnauthorizedOperationException(
                            "Acesso negado: você só pode consultar dados de alunos das turmas em que leciona.");
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void enforceAnnouncementAccess(Announcement announcement) {
        if (announcement == null) return;
        if (isStudent()) {
            if (Boolean.TRUE.equals(announcement.getTargetTeachers())) {
                throw new UnauthorizedOperationException(
                        "Acesso negado: este comunicado é restrito ao corpo docente.");
            }
            if (announcement.getClassRooms() != null && !announcement.getClassRooms().isEmpty()) {
                Student student = getAuthenticatedStudent();
                boolean inTargetClass = announcement.getClassRooms().stream()
                        .anyMatch(cr -> cr != null && cr.getId() != null
                                && enrollmentRepository.existsByStudent_IdAndClassRoom_IdAndActiveTrue(student.getId(), cr.getId()));
                if (!inTargetClass) {
                    throw new UnauthorizedOperationException(
                            "Acesso negado: este comunicado não é destinado à sua turma.");
                }
            }
        }
    }

    // ── Private helpers

    @Override
    public User findAuthenticatedUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

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
