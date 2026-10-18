package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.NotificationMessageDTO;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPublisherService notificationPublisherService;

    @Test
    @DisplayName("notifyUser envia para o canal /topic/user/{id}/notifications com metadados preenchidos")
    void notifyUser_sendsToUserTopicWithFilledMetadata() {
        NotificationMessageDTO dto = NotificationMessageDTO.builder()
                .type("GRADE_LAUNCHED")
                .title("Nova Nota")
                .message("Nota 9.5 em Matemática")
                .build();

        notificationPublisherService.notifyUser(42, dto);

        ArgumentCaptor<NotificationMessageDTO> captor = ArgumentCaptor.forClass(NotificationMessageDTO.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/user/42/notifications"), captor.capture());

        NotificationMessageDTO sent = captor.getValue();
        assertEquals("GRADE_LAUNCHED", sent.getType());
        assertNotNull(sent.getId());
        assertNotNull(sent.getTimestamp());
    }

    @Test
    @DisplayName("notifyStudent delega chamada para notifyUser")
    void notifyStudent_delegatesToNotifyUser() {
        NotificationMessageDTO dto = NotificationMessageDTO.builder()
                .type("ATTENDANCE_RECORDED")
                .title("Frequência")
                .message("Falta em História")
                .build();

        notificationPublisherService.notifyStudent(11, dto);

        verify(messagingTemplate).convertAndSend(eq("/topic/user/11/notifications"), any(NotificationMessageDTO.class));
    }

    @Test
    @DisplayName("notifyClassroom busca IDs dos alunos da turma e envia para cada um")
    void notifyClassroom_findsStudentsAndNotifiesEach() {
        when(userRepository.findStudentIdsByClassroomId(5)).thenReturn(List.of(11, 12, 13));

        NotificationMessageDTO dto = NotificationMessageDTO.builder()
                .type("NEW_ANNOUNCEMENT")
                .title("Aviso da Turma")
                .message("Aviso para o 1º A")
                .build();

        notificationPublisherService.notifyClassroom(5, dto);

        verify(messagingTemplate).convertAndSend(eq("/topic/user/11/notifications"), any(NotificationMessageDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/12/notifications"), any(NotificationMessageDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/13/notifications"), any(NotificationMessageDTO.class));
    }

    @Test
    @DisplayName("notifyTeachers busca IDs dos professores e envia para cada um")
    void notifyTeachers_findsTeachersAndNotifiesEach() {
        when(userRepository.findIdsByRoles(List.of(UserRole.TEACHER))).thenReturn(List.of(101, 102));

        NotificationMessageDTO dto = NotificationMessageDTO.builder()
                .type("NEW_ANNOUNCEMENT")
                .title("Aviso aos Professores")
                .message("Reunião pedagógica às 14h")
                .build();

        notificationPublisherService.notifyTeachers(dto);

        verify(messagingTemplate).convertAndSend(eq("/topic/user/101/notifications"), any(NotificationMessageDTO.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/user/102/notifications"), any(NotificationMessageDTO.class));
    }

    @Test
    @DisplayName("notifyAll envia para o canal global /topic/global/notifications")
    void notifyAll_sendsToGlobalTopic() {
        NotificationMessageDTO dto = NotificationMessageDTO.builder()
                .type("NEW_ANNOUNCEMENT")
                .title("Feriado")
                .message("Não haverá aula na sexta-feira")
                .build();

        notificationPublisherService.notifyAll(dto);

        verify(messagingTemplate).convertAndSend(eq("/topic/global/notifications"), eq(dto));
    }

    @Test
    @DisplayName("notifyUser trata exceções silenciosamente sem propagar erro")
    void notifyUser_handlesExceptionGracefully() {
        doThrow(new RuntimeException("Simulated broker failure"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        NotificationMessageDTO dto = NotificationMessageDTO.builder().title("Teste").build();

        assertDoesNotThrow(() -> notificationPublisherService.notifyUser(99, dto));
    }
}
