package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.service.AiAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.andretti101.escolaweb.repository.ChatMessageHistoryRepository;
import com.andretti101.escolaweb.model.entity.ChatMessageHistory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AiChatController {

    private final AiAgentService aiAgentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMemory chatMemory;
    private final ChatMessageHistoryRepository chatMessageHistoryRepository;

    public record ChatRequest(String message, String conversationId) {}
    public record ChatResponse(String reply, String conversationId) {}
    public record HistoryMessage(String role, String content) {}

    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<HistoryMessage> getHistory(@RequestParam String conversationId) {
        return chatMemory.get(conversationId).stream()
                .filter(m -> m.getMessageType().name().equals("USER") || m.getMessageType().name().equals("ASSISTANT"))
                .map(m -> new HistoryMessage(m.getMessageType().name(), m.getText()))
                .toList();
    }

    @DeleteMapping("/api/chat/history/{conversationId}/after/{messageIndex}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> deleteHistoryAfter(@PathVariable String conversationId, @PathVariable int messageIndex) {
        List<ChatMessageHistory> history = chatMessageHistoryRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int currentFilteredIndex = 0;
        int startIndexToDelete = -1;
        for (int i = 0; i < history.size(); i++) {
            ChatMessageHistory h = history.get(i);
            if (h.getMessageType().equals("USER") || h.getMessageType().equals("ASSISTANT")) {
                if (currentFilteredIndex == messageIndex) {
                    startIndexToDelete = i;
                    break;
                }
                currentFilteredIndex++;
            }
        }
        if (startIndexToDelete != -1) {
            chatMessageHistoryRepository.deleteAll(history.subList(startIndexToDelete, history.size()));
        }
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/ai/chat")
    public void handleAiChat(@Payload ChatRequest request, Principal principal) {
        if (principal instanceof Authentication auth) {
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        try {
            String username = principal != null ? principal.getName() : "anonymous";
            String conversationId = (request.conversationId() != null && !request.conversationId().isBlank()) 
                    ? request.conversationId() 
                    : username;
            
            String reply = aiAgentService.chat(request.message(), conversationId);
            
            messagingTemplate.convertAndSendToUser(
                    username, 
                    "/queue/ai/reply", 
                    new ChatResponse(reply, conversationId)
            );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
