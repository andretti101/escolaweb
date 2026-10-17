package com.andretti101.escolaweb;

import com.andretti101.escolaweb.config.WebSocketAuthInterceptor;
import com.andretti101.escolaweb.controller.ChatController;
import com.andretti101.escolaweb.dto.request.ChatMessageRequestDTO;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.StudentAiTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditoriaCorrecoesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChatController chatController;

    @Autowired
    private StudentAiTools studentAiTools;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private MessageChannel messageChannel;

    // ── 1. SecurityConfig HTML Redirections & API Protection ───────────────────

    @Test
    @DisplayName("Acesso não autenticado a rotas HTML administrativas e de perfil redireciona para login.html")
    void unauthenticatedAccessToHtml_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/principal/dashboard.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login.html"));

        mockMvc.perform(get("/secretary/dashboard.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login.html"));

        mockMvc.perform(get("/teacher/dashboard.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login.html"));

        mockMvc.perform(get("/student/dashboard.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login.html"));
    }

    @Test
    @DisplayName("Acesso não autenticado a endpoints REST /api/** retorna 401 Unauthorized")
    void unauthenticatedAccessToApi_returns401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    // ── 2. AiChatController IDOR Prevention ──────────────────────────────────

    @Test
    @WithMockUser(username = "aluno.teste@example.com", roles = "STUDENT")
    @DisplayName("AiChatController: consulta de histórico de outro usuário retorna 403 Forbidden")
    void aiChat_getHistoryOtherUser_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/chat/history")
                        .param("conversationId", "outro.usuario@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "aluno.teste@example.com", roles = "STUDENT")
    @DisplayName("AiChatController: consulta de histórico com prefixo conv-{userId} do próprio usuário retorna 200 OK")
    void aiChat_getHistoryOwnConvFormat_returnsOk() throws Exception {
        Student student = Student.builder()
                .id(11)
                .email("aluno.teste@example.com")
                .role(UserRole.STUDENT)
                .build();
        when(userRepository.findByEmail("aluno.teste@example.com")).thenReturn(Optional.of(student));

        mockMvc.perform(get("/api/chat/history")
                        .param("conversationId", "conv-11-1729094000000"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "aluno.teste@example.com", roles = "STUDENT")
    @DisplayName("AiChatController: consulta de histórico com prefixo conv-{otherUserId} de outro usuário retorna 403 Forbidden")
    void aiChat_getHistoryOtherConvFormat_returnsForbidden() throws Exception {
        Student student = Student.builder()
                .id(11)
                .email("aluno.teste@example.com")
                .role(UserRole.STUDENT)
                .build();
        when(userRepository.findByEmail("aluno.teste@example.com")).thenReturn(Optional.of(student));

        mockMvc.perform(get("/api/chat/history")
                        .param("conversationId", "conv-99-1729094000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "aluno.teste@example.com", roles = "STUDENT")
    @DisplayName("AiChatController: deleção de histórico de outro usuário retorna 403 Forbidden")
    void aiChat_deleteHistoryOtherUser_returnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/chat/history/{conversationId}/after/{index}", "outro.usuario@example.com", 0))
                .andExpect(status().isForbidden());
    }

    // ── 3. ChatController Teacher Room Role Enforcement ───────────────────────

    @Test
    @DisplayName("ChatController: estudante não pode enviar, editar, excluir ou marcar como lidas mensagens na sala dos professores")
    void chatController_studentCannotPostInTeacherRoom() {
        Student student = Student.builder()
                .id(11)
                .email("aluno@example.com")
                .role(UserRole.STUDENT)
                .build();

        when(userRepository.findByEmail("aluno@example.com")).thenReturn(Optional.of(student));

        Principal studentPrincipal = () -> "aluno@example.com";
        ChatMessageRequestDTO dto = new ChatMessageRequestDTO();
        dto.setContent("Olá professores!");

        assertThrows(AccessDeniedException.class, () ->
                chatController.sendTeacherMessage(dto, studentPrincipal));

        assertThrows(AccessDeniedException.class, () ->
                chatController.editTeacherMessage(1, dto, studentPrincipal));

        assertThrows(AccessDeniedException.class, () ->
                chatController.deleteTeacherMessage(1, studentPrincipal));

        org.springframework.http.ResponseEntity<Void> readResponse = chatController.markTeacherAsRead(1, studentPrincipal);
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, readResponse.getStatusCode());
    }

    // ── 4. WebSocketAuthInterceptor Private Notifications Protection ──────────

    @Test
    @DisplayName("WebSocketAuthInterceptor: estudante não pode assinar notificações de outro usuário")
    void webSocket_cannotSubscribeToOtherUserNotifications() {
        Student student = Student.builder()
                .id(11)
                .email("aluno@example.com")
                .role(UserRole.STUDENT)
                .build();

        when(userRepository.findByEmail("aluno@example.com")).thenReturn(Optional.of(student));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new UsernamePasswordAuthenticationToken("aluno@example.com", null));
        accessor.setDestination("/topic/user/99/notifications");
        accessor.setLeaveMutable(true);

        org.springframework.messaging.Message<byte[]> msg =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () ->
                webSocketAuthInterceptor.preSend(msg, messageChannel));

        // Tentar assinar /topic/user/99 sem sufixo também deve ser bloqueado
        accessor.setDestination("/topic/user/99");
        org.springframework.messaging.Message<byte[]> msgNoSuffix =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () ->
                webSocketAuthInterceptor.preSend(msgNoSuffix, messageChannel));

        // Assinar o próprio canal de notificações deve ser permitido
        accessor.setDestination("/topic/user/11/notifications");
        org.springframework.messaging.Message<byte[]> ownMsg =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertDoesNotThrow(() -> webSocketAuthInterceptor.preSend(ownMsg, messageChannel));
    }

    // ── 5. StudentAiTools Safe Fallback for Non-Students ───────────────────────

    @Test
    @DisplayName("StudentAiTools: retorna mensagem orientativa quando invocado por não-alunos sem lançar exceção")
    void studentAiTools_safeFallbackWhenNotStudent() {
        when(authenticatedUserService.isStudent()).thenReturn(false);
        when(authenticatedUserService.getAuthenticatedStudent()).thenThrow(new RuntimeException("Não é aluno"));

        String grades = studentAiTools.getStudentGrades();
        assertTrue(grades.contains("disponível quando acessada pelo perfil de um estudante."));

        String attendance = studentAiTools.getStudentAttendance();
        assertTrue(attendance.contains("disponível quando acessada pelo perfil de um estudante."));

        String reportCard = studentAiTools.getStudentReportCard();
        assertTrue(reportCard.contains("disponível quando acessada pelo perfil de um estudante."));

        String points = studentAiTools.getStudentPointsNeeded();
        assertTrue(points.contains("disponível quando acessada pelo perfil de um estudante."));

        String classmates = studentAiTools.getStudentClassmates();
        assertTrue(classmates.contains("disponível quando acessada pelo perfil de um estudante."));

        String enrollment = studentAiTools.getStudentEnrollmentDetails();
        assertTrue(enrollment.contains("disponível quando acessada pelo perfil de um estudante."));

        String teachers = studentAiTools.getStudentTeachers();
        assertTrue(teachers.contains("disponível quando acessada pelo perfil de um estudante."));

        String analysis = studentAiTools.getStudentSubjectAnalysis();
        assertTrue(analysis.contains("disponível quando acessada pelo perfil de um estudante."));
    }
}
