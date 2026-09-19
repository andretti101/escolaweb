package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ChatMessageRequestDTO;
import com.andretti101.escolaweb.dto.response.ChatMessageResponseDTO;
import com.andretti101.escolaweb.model.entity.ChatMessage;
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

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/classroom/{classroomId}/send")
    public void sendMessage(@DestinationVariable Integer classroomId,
                            @Payload ChatMessageRequestDTO messageRequest,
                            Principal principal) {
        
        // Obter o usuário logado
        User sender = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        // Salvar a mensagem no banco
        ChatMessage savedMessage = chatMessageService.saveMessage(classroomId, sender.getId(), messageRequest.getContent());

        // Converter para DTO de resposta
        ChatMessageResponseDTO responseDTO = mapToResponseDTO(savedMessage);

        // Enviar para os inscritos no tópico da turma
        messagingTemplate.convertAndSend("/topic/classroom/" + classroomId, responseDTO);
    }

    @GetMapping("/classroom/{classroomId}/history")
    public ResponseEntity<List<ChatMessageResponseDTO>> getHistory(@PathVariable Integer classroomId) {
        List<ChatMessage> history = chatMessageService.getHistoryByClassroomId(classroomId);
        List<ChatMessageResponseDTO> responseDTOs = history.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    private ChatMessageResponseDTO mapToResponseDTO(ChatMessage message) {
        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .classroomId(message.getClassroom().getId())
                .build();
    }
}
