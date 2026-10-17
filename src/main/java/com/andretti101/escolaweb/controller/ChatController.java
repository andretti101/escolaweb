package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ChatMessageRequestDTO;
import com.andretti101.escolaweb.dto.response.ChatMessageResponseDTO;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import java.security.Principal;
import java.util.List;

@Tag(name = "Chat e Mensagens", description = "Endpoints para consulta de histórico, membros, controle de leitura, moderação e bloqueio no chat de turmas e sala dos professores")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final com.andretti101.escolaweb.repository.ClassRoomRepository classRoomRepository;
    private final com.andretti101.escolaweb.repository.StudentRepository studentRepository;
    private final com.andretti101.escolaweb.repository.EnrollmentRepository enrollmentRepository;

    private void validateUserAccess(User user, Integer classroomId) {
        String role = user.getRole() != null ? user.getRole().name() : "";
        if (role.equals("PRINCIPAL") || role.equals("SECRETARY")) {
            return; // Admins have global access
        }
        
        if (role.equals("STUDENT")) {
            boolean enrolled = enrollmentRepository.existsByStudent_IdAndClassRoom_IdAndActiveTrue(user.getId(), classroomId);
            if (!enrolled) {
                throw new AccessDeniedException("Você não pertence a esta turma.");
            }
        } else if (role.equals("TEACHER")) {
            throw new AccessDeniedException("Acesso negado: Professores não têm permissão para acessar o chat da turma.");
        } else {
            throw new AccessDeniedException("Acesso negado.");
        }
    }

    @MessageMapping("/classroom/{classroomId}/send")
    public void sendMessage(@DestinationVariable("classroomId") Integer classroomId,
                            @Valid @Payload ChatMessageRequestDTO messageRequest,
                            Principal principal) {
        
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        validateUserAccess(sender, classroomId);

        ChatMessageResponseDTO responseDTO = chatMessageService.saveMessage(
                classroomId, sender.getId(), messageRequest.getContent(), messageRequest.getRepliedToId());

        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, responseDTO);

        // Disparar notificação em tempo real para os usuários envolvidos na turma
        List<Integer> usersToNotify = chatMessageService.getUsersToNotify(classroomId, sender.getId());
        usersToNotify.forEach(userId -> {
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", "NEW_CHAT_MESSAGE");
        });
    }

    @MessageMapping("/classroom/{classroomId}/edit/{messageId}")
    public void editMessage(@DestinationVariable("classroomId") Integer classroomId,
                            @DestinationVariable("messageId") Integer messageId,
                            @Valid @Payload ChatMessageRequestDTO editRequest,
                            Principal principal) {
        
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        validateUserAccess(sender, classroomId);

        ChatMessageResponseDTO responseDTO = chatMessageService.editMessage(
                messageId, sender.getId(), editRequest.getContent());
                
        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, responseDTO);
    }

    @MessageMapping("/classroom/{classroomId}/delete/{messageId}")
    public void deleteMessage(@DestinationVariable("classroomId") Integer classroomId,
                              @DestinationVariable("messageId") Integer messageId,
                              Principal principal) {
        
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        validateUserAccess(sender, classroomId);

        boolean isAdmin = sender.getRole() != null && 
                (sender.getRole().name().equals("SECRETARY") || sender.getRole().name().equals("PRINCIPAL"));

        ChatMessageResponseDTO responseDTO = chatMessageService.deleteMessage(messageId, sender.getId(), isAdmin);
        
        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, responseDTO);
    }

    @Operation(summary = "Obter histórico do chat da turma", description = "Retorna a lista de mensagens recentes da turma especificada, o ID da última mensagem lida pelo usuário e o status de bloqueio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado caso o usuário não pertença à turma"),
            @ApiResponse(responseCode = "404", description = "Usuário ou turma não encontrado")
    })
    @GetMapping("/classroom/{classroomId}/history")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.ChatHistoryResponseDTO> getHistory(
            @Parameter(description = "ID da turma", example = "1", required = true) @PathVariable Integer classroomId,
            Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
        validateUserAccess(user, classroomId);

        List<ChatMessageResponseDTO> history = chatMessageService.getHistoryByClassroomId(classroomId, user.getId());
        Integer lastReadId = chatMessageService.getLastReadMessageId(user.getId(), classroomId);
        
        boolean isBlocked = false;
        if (user instanceof com.andretti101.escolaweb.model.entity.Student) {
            isBlocked = ((com.andretti101.escolaweb.model.entity.Student) user).isChatBlocked();
        }

        com.andretti101.escolaweb.dto.response.ChatHistoryResponseDTO response = com.andretti101.escolaweb.dto.response.ChatHistoryResponseDTO.builder()
                .messages(history)
                .lastReadMessageId(lastReadId)
                .isChatBlocked(isBlocked)
                .build();
                
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verificar mensagens não lidas", description = "Verifica se o usuário autenticado possui mensagens não lidas em suas turmas ou na sala dos professores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de leitura retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/unread-status")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.UnreadStatusDTO> getUnreadStatus(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
                
        boolean hasUnread = chatMessageService.hasUnreadMessages(user.getId()) || 
                            chatMessageService.hasUnreadGlobalTeacherMessages(user.getId());
        return ResponseEntity.ok(new com.andretti101.escolaweb.dto.response.UnreadStatusDTO(hasUnread));
    }

    @Operation(summary = "Marcar mensagens da turma como lidas", description = "Atualiza o ponteiro de leitura da turma até o ID da mensagem indicada para o usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagens marcadas como lidas com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado caso o usuário não pertença à turma"),
            @ApiResponse(responseCode = "404", description = "Usuário ou turma não encontrado")
    })
    @PostMapping("/read/{classroomId}")
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "ID da turma", example = "1", required = true) @PathVariable Integer classroomId,
            @Parameter(description = "ID da mensagem até a qual a leitura foi feita", example = "10", required = true) @RequestParam Integer messageId,
            Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
        validateUserAccess(user, classroomId);
        
        chatMessageService.markAsRead(user.getId(), classroomId, messageId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Listar membros do chat da turma", description = "Retorna os alunos matriculados na turma. Administradores visualizam matrícula e status de bloqueio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de membros retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado caso o usuário não pertença à turma"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @GetMapping("/classrooms/{id}/members")
    public ResponseEntity<List<com.andretti101.escolaweb.dto.response.ChatMemberDTO>> getMembers(
            @Parameter(description = "ID da turma", example = "1", required = true) @PathVariable Integer id,
            Principal principal) {
        User requester = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        validateUserAccess(requester, id);
        boolean isAdmin = requester.getRole() == com.andretti101.escolaweb.model.enums.UserRole.SECRETARY || 
                          requester.getRole() == com.andretti101.escolaweb.model.enums.UserRole.PRINCIPAL;

        com.andretti101.escolaweb.model.entity.ClassRoom classroom = classRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turma não encontrada."));

        List<com.andretti101.escolaweb.dto.response.ChatMemberDTO> members = new java.util.ArrayList<>();

        // Add students
        studentRepository.findByClassroomId(id).forEach(s -> {
            com.andretti101.escolaweb.dto.response.ChatMemberDTO dto = com.andretti101.escolaweb.dto.response.ChatMemberDTO.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .build();
            if (isAdmin) {
                dto.setRegistrationNumber(s.getRegistrationNumber());
                dto.setIsBlocked(s.isChatBlocked());
            }
            members.add(dto);
        });

        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Listar membros da sala dos professores", description = "Retorna a lista de professores da instituição. Acesso restrito a professores e gestão escolar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de membros retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para estudantes"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/teachers/members")
    public ResponseEntity<List<com.andretti101.escolaweb.dto.response.ChatMemberDTO>> getTeacherMembers(Principal principal) {
        User requester = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
        if (requester.getRole() == com.andretti101.escolaweb.model.enums.UserRole.STUDENT) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        List<com.andretti101.escolaweb.dto.response.ChatMemberDTO> members = new java.util.ArrayList<>();

        userRepository.findByRole(com.andretti101.escolaweb.model.enums.UserRole.TEACHER).forEach(t -> {
            com.andretti101.escolaweb.dto.response.ChatMemberDTO dto = com.andretti101.escolaweb.dto.response.ChatMemberDTO.builder()
                    .id(t.getId())
                    .name(t.getName())
                    .email(t.getEmail())
                    .build();
            members.add(dto);
        });

        return ResponseEntity.ok(members);
    }

    @Operation(summary = "Ocultar mensagem", description = "Oculta uma mensagem específica para o usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagem ocultada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário ou mensagem não encontrado")
    })
    @PostMapping("/messages/{messageId}/hide")
    public ResponseEntity<Void> hideMessage(
            @Parameter(description = "ID da mensagem", example = "100", required = true) @PathVariable Integer messageId,
            Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        chatMessageService.hideMessage(messageId, user.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bloquear ou desbloquear chat do aluno", description = "Alterna o status de bloqueio do chat para um estudante. Apenas SECRETARY e PRINCIPAL têm permissão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de bloqueio atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aluno não encontrado")
    })
    @PutMapping("/users/{studentId}/block")
    public ResponseEntity<Void> blockStudent(
            @Parameter(description = "ID do aluno", example = "11", required = true) @PathVariable Integer studentId,
            Principal principal) {
        User requester = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (requester.getRole() != com.andretti101.escolaweb.model.enums.UserRole.SECRETARY && 
            requester.getRole() != com.andretti101.escolaweb.model.enums.UserRole.PRINCIPAL) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }

        com.andretti101.escolaweb.model.entity.Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado."));
        
        student.setChatBlocked(!student.isChatBlocked());
        studentRepository.save(student);
        return ResponseEntity.ok().build();
    }

    @org.springframework.messaging.handler.annotation.MessageExceptionHandler({IllegalArgumentException.class, AccessDeniedException.class})
    @org.springframework.messaging.simp.annotation.SendToUser("/queue/errors")
    public String handleException(Exception exception) {
        return exception.getMessage();
    }

    @MessageMapping("/teachers/send")
    public void sendTeacherMessage(@Valid @Payload ChatMessageRequestDTO messageRequest,
                                   Principal principal) {
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (sender.getRole() == com.andretti101.escolaweb.model.enums.UserRole.STUDENT) {
            throw new AccessDeniedException("Acesso negado: Estudantes não têm permissão para enviar mensagens na sala dos professores.");
        }

        ChatMessageResponseDTO responseDTO = chatMessageService.saveGlobalTeacherMessage(
                sender.getId(), messageRequest.getContent(), messageRequest.getRepliedToId());

        messagingTemplate.convertAndSend("/topic/teachers", responseDTO);

        // Disparar notificação privada para quem tem acesso à sala dos professores
        List<com.andretti101.escolaweb.model.enums.UserRole> roles = java.util.Arrays.asList(
            com.andretti101.escolaweb.model.enums.UserRole.TEACHER,
            com.andretti101.escolaweb.model.enums.UserRole.SECRETARY,
            com.andretti101.escolaweb.model.enums.UserRole.PRINCIPAL
        );
        userRepository.findIdsByRoles(roles).stream()
                .filter(id -> !id.equals(sender.getId()))
                .forEach(id -> {
                    messagingTemplate.convertAndSend("/topic/user/" + id + "/notifications", "NEW_GLOBAL_TEACHER_MESSAGE");
                });
    }

    @MessageMapping("/teachers/edit/{messageId}")
    public void editTeacherMessage(@DestinationVariable("messageId") Integer messageId,
                                   @Valid @Payload ChatMessageRequestDTO editRequest,
                                   Principal principal) {
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (sender.getRole() == com.andretti101.escolaweb.model.enums.UserRole.STUDENT) {
            throw new AccessDeniedException("Acesso negado: Estudantes não têm permissão na sala dos professores.");
        }

        ChatMessageResponseDTO responseDTO = chatMessageService.editMessage(
                messageId, sender.getId(), editRequest.getContent());
                
        messagingTemplate.convertAndSend("/topic/teachers", responseDTO);
    }

    @MessageMapping("/teachers/delete/{messageId}")
    public void deleteTeacherMessage(@DestinationVariable("messageId") Integer messageId,
                                     Principal principal) {
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        if (sender.getRole() == com.andretti101.escolaweb.model.enums.UserRole.STUDENT) {
            throw new AccessDeniedException("Acesso negado: Estudantes não têm permissão na sala dos professores.");
        }

        boolean isAdmin = sender.getRole() != null && 
                (sender.getRole().name().equals("SECRETARY") || sender.getRole().name().equals("PRINCIPAL"));

        ChatMessageResponseDTO responseDTO = chatMessageService.deleteMessage(messageId, sender.getId(), isAdmin);
        
        messagingTemplate.convertAndSend("/topic/teachers", responseDTO);
    }

    @Operation(summary = "Obter histórico da sala dos professores", description = "Retorna o histórico de mensagens da sala dos professores e o ID da última mensagem lida. Acesso restrito a professores e gestão escolar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para alunos"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/teachers/history")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.ChatHistoryResponseDTO> getTeacherHistory(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
        if (user.getRole() == com.andretti101.escolaweb.model.enums.UserRole.STUDENT) {
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado: Alunos não têm acesso ao chat dos professores.");
        }

        List<ChatMessageResponseDTO> history = chatMessageService.getGlobalTeacherHistory(user.getId());
        Integer lastReadId = chatMessageService.getGlobalTeacherLastReadMessageId(user.getId());

        com.andretti101.escolaweb.dto.response.ChatHistoryResponseDTO response = com.andretti101.escolaweb.dto.response.ChatHistoryResponseDTO.builder()
                .messages(history)
                .lastReadMessageId(lastReadId)
                .isChatBlocked(false) // Professores nunca são bloqueados
                .build();
                
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Marcar mensagens dos professores como lidas", description = "Atualiza o ponteiro de leitura da sala dos professores até a mensagem indicada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensagens marcadas como lidas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @PostMapping("/teachers/read")
    public ResponseEntity<Void> markTeacherAsRead(
            @Parameter(description = "ID da última mensagem lida", example = "50", required = true) @RequestParam Integer messageId,
            Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
        if (user.getRole() == com.andretti101.escolaweb.model.enums.UserRole.STUDENT) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
                
        chatMessageService.markGlobalTeacherAsRead(user.getId(), messageId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Obter histórico arquivado do chat da turma", description = "Retorna o histórico arquivado de mensagens de uma turma específica. Exclusivo para SECRETARY e PRINCIPAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico arquivado retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/classroom/{classroomId}/archived-history")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<ChatMessageResponseDTO>> getArchivedHistory(
            @Parameter(description = "ID da turma", example = "1", required = true) @PathVariable Integer classroomId) {
        return ResponseEntity.ok(chatMessageService.getArchivedHistoryByClassroomId(classroomId));
    }

    @Operation(summary = "Obter histórico arquivado da sala dos professores", description = "Retorna o histórico arquivado de mensagens da sala dos professores, opcionalmente filtrado por ano. Exclusivo para SECRETARY e PRINCIPAL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico arquivado retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/teachers/archived-history")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<ChatMessageResponseDTO>> getArchivedTeacherHistory(
            @Parameter(description = "Ano de referência para filtro", example = "2026") @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(chatMessageService.getArchivedGlobalTeacherHistory(year));
    }
}
