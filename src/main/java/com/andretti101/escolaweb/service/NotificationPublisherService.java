package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.NotificationMessageDTO;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisherService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public void notifyUser(Integer userId, NotificationMessageDTO dto) {
        if (userId == null || dto == null) return;
        ensureMetadata(dto);
        try {
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", dto);
        } catch (Exception e) {
            log.warn("Falha ao enviar notificação para o usuário {}: {}", userId, e.getMessage());
        }
    }

    public void notifyStudent(Integer studentId, NotificationMessageDTO dto) {
        notifyUser(studentId, dto);
    }

    public void notifyClassroom(Integer classroomId, NotificationMessageDTO dto) {
        if (classroomId == null || dto == null) return;
        ensureMetadata(dto);
        try {
            List<Integer> studentIds = userRepository.findStudentIdsByClassroomId(classroomId);
            if (studentIds != null) {
                for (Integer studentId : studentIds) {
                    notifyUser(studentId, dto);
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar notificações para a turma {}: {}", classroomId, e.getMessage());
        }
    }

    public void notifyTeachers(NotificationMessageDTO dto) {
        if (dto == null) return;
        ensureMetadata(dto);
        try {
            List<Integer> teacherIds = userRepository.findIdsByRoles(List.of(UserRole.TEACHER));
            if (teacherIds != null) {
                for (Integer teacherId : teacherIds) {
                    notifyUser(teacherId, dto);
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar notificações para professores: {}", e.getMessage());
        }
    }

    public void notifyAll(NotificationMessageDTO dto) {
        if (dto == null) return;
        ensureMetadata(dto);
        try {
            messagingTemplate.convertAndSend("/topic/global/notifications", dto);
        } catch (Exception e) {
            log.warn("Falha ao enviar notificação global: {}", e.getMessage());
        }
    }

    private void ensureMetadata(NotificationMessageDTO dto) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            dto.setId(UUID.randomUUID().toString());
        }
        if (dto.getTimestamp() == null) {
            dto.setTimestamp(LocalDateTime.now());
        }
    }
}
