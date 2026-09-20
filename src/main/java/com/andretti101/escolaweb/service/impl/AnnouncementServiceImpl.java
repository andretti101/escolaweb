package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Announcement;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.TeacherService;
import com.andretti101.escolaweb.repository.AnnouncementRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.service.AnnouncementService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.ClassRoomService;
import com.andretti101.escolaweb.service.StudentService;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.model.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ClassRoomService classRoomService;
    private final com.andretti101.escolaweb.repository.ClassRoomRepository classRoomRepository;
    private final StudentService studentService;
    private final EnrollmentRepository enrollmentRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final TeacherService teacherService;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final com.andretti101.escolaweb.repository.UserRepository userRepository;

    @Override
    @Transactional
    public Announcement create(Announcement announcement, List<Integer> classRoomIds) {
        User authUser = authenticatedUserService.getAuthenticatedUser();
        if (Boolean.TRUE.equals(announcement.getTargetTeachers()) && authUser.getRole() == UserRole.TEACHER) {
            throw new org.springframework.security.access.AccessDeniedException("Professores não podem enviar recados para professores.");
        }
        
        announcement.setAuthor(authUser);

        if (!Boolean.TRUE.equals(announcement.getTargetTeachers()) && (classRoomIds == null || classRoomIds.isEmpty())) {
            throw new IllegalArgumentException("Selecione pelo menos uma turma ou marque a opção para professores.");
        }

        if (authUser.getRole() == UserRole.TEACHER && classRoomIds != null && !classRoomIds.isEmpty()) {
            Teacher teacher = teacherService.findById(authUser.getId());
            List<Integer> allowedIds = teacherClassSubjectRepository.findByTeacher(teacher)
                    .stream().map(tcs -> tcs.getClassRoom().getId()).toList();
            for (Integer cid : classRoomIds) {
                if (!allowedIds.contains(cid)) {
                    throw new org.springframework.security.access.AccessDeniedException("Você não tem permissão para enviar recados para turmas que não leciona.");
                }
            }
        }

        Set<ClassRoom> classRooms = new HashSet<>();
        if (classRoomIds != null && !classRoomIds.isEmpty()) {
            List<ClassRoom> fetched = classRoomRepository.findAllById(classRoomIds);
            if (fetched.size() != classRoomIds.size()) {
                throw new EntityNotFoundException("Uma ou mais turmas não foram encontradas.");
            }
            classRooms.addAll(fetched);
        }
        announcement.setClassRooms(classRooms);

        return announcementRepository.save(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public Announcement findById(Integer id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Aviso não encontrado com id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> findAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> findByAuthorId(Integer authorId) {
        return announcementRepository.findByAuthorIdOrderByEventDateAsc(authorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> findByStudentClassRooms(Integer studentId) {
        Student student = studentService.findById(studentId);
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentAndActiveTrue(student);

        if (activeEnrollments.isEmpty()) {
            return List.of();
        }

        List<Integer> classRoomIds = activeEnrollments.stream()
                .map(e -> e.getClassRoom().getId())
                .toList();

        return announcementRepository.findByClassRoomIds(classRoomIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Announcement> findByTeacherClassRooms(Integer teacherId) {
        Teacher teacher = teacherService.findById(teacherId);
        List<TeacherClassSubject> assignments = teacherClassSubjectRepository.findByTeacher(teacher);
        List<Integer> classRoomIds = assignments.stream().map(a -> a.getClassRoom().getId()).toList();
        if (classRoomIds.isEmpty()) return List.of();
        return announcementRepository.findByClassRoomIds(classRoomIds);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Announcement> findForTeachers() {
        return announcementRepository.findByTargetTeachersTrue();
    }


    @Override
    @Transactional
    public Announcement update(Integer id, Announcement updated, List<Integer> classRoomIds, Integer authenticatedUserId) {
        User authUser = authenticatedUserService.getAuthenticatedUser();
        if (Boolean.TRUE.equals(updated.getTargetTeachers()) && authUser.getRole() == UserRole.TEACHER) {
            throw new org.springframework.security.access.AccessDeniedException("Professores não podem enviar recados para professores.");
        }
        Announcement existing = findById(id);

        boolean canEdit = existing.getAuthor().getId().equals(authenticatedUserId) || 
                          authUser.getRole() == UserRole.PRINCIPAL || 
                          authUser.getRole() == UserRole.SECRETARY;
        
        if (!canEdit) {
            throw new SecurityException("Você não tem permissão para editar este aviso.");
        }

        existing.setTitle(updated.getTitle());
        existing.setMessage(updated.getMessage());
        existing.setEventDate(updated.getEventDate());
        existing.setTargetTeachers(updated.getTargetTeachers());

        if (!Boolean.TRUE.equals(existing.getTargetTeachers()) && (classRoomIds == null || classRoomIds.isEmpty())) {
            throw new IllegalArgumentException("Selecione pelo menos uma turma ou marque a opção para professores.");
        }

        if (authUser.getRole() == UserRole.TEACHER && classRoomIds != null && !classRoomIds.isEmpty()) {
            Teacher teacher = teacherService.findById(authUser.getId());
            List<Integer> allowedIds = teacherClassSubjectRepository.findByTeacher(teacher)
                    .stream().map(tcs -> tcs.getClassRoom().getId()).toList();
            for (Integer cid : classRoomIds) {
                if (!allowedIds.contains(cid)) {
                    throw new org.springframework.security.access.AccessDeniedException("Você não tem permissão para enviar recados para turmas que não leciona.");
                }
            }
        }

        Set<ClassRoom> classRooms = new HashSet<>();
        if (classRoomIds != null && !classRoomIds.isEmpty()) {
            List<ClassRoom> fetched = classRoomRepository.findAllById(classRoomIds);
            if (fetched.size() != classRoomIds.size()) {
                throw new EntityNotFoundException("Uma ou mais turmas não foram encontradas.");
            }
            classRooms.addAll(fetched);
        }
        existing.setClassRooms(classRooms);

        return announcementRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id, Integer userId) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Aviso não encontrado com id: " + id));
        
        User authUser = userRepository.findById(userId).orElseThrow();
        
        boolean canDelete = announcement.getAuthor().getId().equals(userId) || 
                            authUser.getRole() == UserRole.PRINCIPAL || 
                            authUser.getRole() == UserRole.SECRETARY;
                            
        if (!canDelete) {
            throw new SecurityException("Você não tem permissão para excluir este aviso.");
        }
        announcementRepository.delete(announcement);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUnreadAnnouncements(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        UserRole role = user.getRole();
        java.time.LocalDateTime lastRead = user.getLastReadAnnouncements() != null 
                ? user.getLastReadAnnouncements() 
                : java.time.LocalDateTime.MIN;
        
        if (role == UserRole.STUDENT) {
            Student student = studentService.findById(userId);
            List<Integer> classRoomIds = enrollmentRepository.findByStudentAndActiveTrue(student)
                    .stream().map(e -> e.getClassRoom().getId()).toList();
            if (classRoomIds.isEmpty()) return false;
            return announcementRepository.countUnreadByClassRoomIds(classRoomIds, userId, lastRead) > 0;
        } else if (role == UserRole.TEACHER) {
            Teacher teacher = teacherService.findById(userId);
            List<Integer> classRoomIds = teacherClassSubjectRepository.findByTeacher(teacher)
                    .stream().map(tcs -> tcs.getClassRoom().getId()).distinct().toList();
            
            long unreadForTeachers = announcementRepository.countUnreadForTeachers(userId, lastRead);
            if (unreadForTeachers > 0) return true;
            
            if (classRoomIds.isEmpty()) return false;
            return announcementRepository.countUnreadByClassRoomIds(classRoomIds, userId, lastRead) > 0;
        } else {
            return announcementRepository.countAllUnread(userId, lastRead) > 0;
        }
    }

    @Override
    @Transactional
    public void markAnnouncementsAsRead(Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setLastReadAnnouncements(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Announcement> findWithFilters(Integer authorId, Integer classRoomId, Boolean targetTeachers, org.springframework.data.domain.Pageable pageable) {
        return announcementRepository.findWithFilters(authorId, classRoomId, targetTeachers, pageable);
    }
}