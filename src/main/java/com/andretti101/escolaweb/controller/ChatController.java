package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ChatMessageRequestDTO;
import com.andretti101.escolaweb.dto.response.ChatMessageResponseDTO;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final com.andretti101.escolaweb.repository.ClassRoomRepository classRoomRepository;
    private final com.andretti101.escolaweb.repository.StudentRepository studentRepository;
    private final com.andretti101.escolaweb.repository.TeacherClassSubjectRepository teacherClassSubjectRepository;

    private void validateUserAccess(User user, Integer classroomId) {
        String role = user.getRole() != null ? user.getRole().name() : "";
        if (role.equals("PRINCIPAL") || role.equals("SECRETARY")) {
            return; // Admins have global access
        }
        
        if (role.equals("STUDENT")) {
            List<Integer> enrolledIds = userRepository.findStudentIdsByClassroomId(classroomId);
            if (!enrolledIds.contains(user.getId())) {
                throw new AccessDeniedException("Você não pertence a esta turma.");
            }
        } else if (role.equals("TEACHER")) {
            if (!teacherClassSubjectRepository.existsByTeacher_IdAndClassRoom_Id(user.getId(), classroomId)) {
                throw new AccessDeniedException("Você não leciona nesta turma.");
            }
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

    @GetMapping("/classroom/{classroomId}/history")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.ChatHistoryResponseDTO> getHistory(@PathVariable Integer classroomId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
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

    @GetMapping("/unread-status")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.UnreadStatusDTO> getUnreadStatus(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
                
        boolean hasUnread = chatMessageService.hasUnreadMessages(user.getId());
        return ResponseEntity.ok(new com.andretti101.escolaweb.dto.response.UnreadStatusDTO(hasUnread));
    }

    @PostMapping("/read/{classroomId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Integer classroomId, @RequestParam Integer messageId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
                
        chatMessageService.markAsRead(user.getId(), classroomId, messageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/classrooms/{id}/members")
    public ResponseEntity<List<com.andretti101.escolaweb.dto.response.ChatMemberDTO>> getMembers(@PathVariable Integer id, Principal principal) {
        User requester = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        boolean isAdmin = requester.getRole() == com.andretti101.escolaweb.model.enums.UserRole.SECRETARY || 
                          requester.getRole() == com.andretti101.escolaweb.model.enums.UserRole.PRINCIPAL;

        com.andretti101.escolaweb.model.entity.ClassRoom classroom = classRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turma não encontrada."));

        List<com.andretti101.escolaweb.dto.response.ChatMemberDTO> members = new java.util.ArrayList<>();

        // Add students
        studentRepository.findAll().stream()
                .filter(s -> s.getEnrollments().stream().anyMatch(e -> e.getClassRoom().getId().equals(id)))
                .forEach(s -> {
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

    @PostMapping("/messages/{messageId}/hide")
    public ResponseEntity<Void> hideMessage(@PathVariable Integer messageId, Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        chatMessageService.hideMessage(messageId, user.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{studentId}/block")
    public ResponseEntity<Void> blockStudent(@PathVariable Integer studentId, Principal principal) {
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
    @org.springframework.messaging.handler.annotation.MessageExceptionHandler
    @org.springframework.messaging.simp.annotation.SendToUser("/queue/errors")
    public String handleException(IllegalArgumentException exception) {
        return exception.getMessage();
    }
}

