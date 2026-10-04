package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.ChatMessageHistory;
import com.andretti101.escolaweb.repository.ChatMessageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JpaChatMemory implements ChatMemory {

    private final ChatMessageHistoryRepository repository;

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        List<ChatMessageHistory> entities = messages.stream().map(msg -> {
            String type = "USER";
            if (msg instanceof AssistantMessage) type = "ASSISTANT";
            else if (msg instanceof SystemMessage) type = "SYSTEM";
            
            return ChatMessageHistory.builder()
                    .conversationId(conversationId)
                    .messageType(type)
                    .content(msg.getText())
                    .createdAt(LocalDateTime.now())
                    .build();
        }).collect(Collectors.toList());
        repository.saveAll(entities);
    }

    public List<Message> get(String conversationId, int lastN) {
        List<Message> history = get(conversationId);
        if (history.size() > lastN) {
            return history.subList(history.size() - lastN, history.size());
        }
        return history;
    }

    @Override
    public List<Message> get(String conversationId) {
        List<ChatMessageHistory> history = repository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return history.stream().map(h -> {
            return switch (h.getMessageType()) {
                case "ASSISTANT" -> new AssistantMessage(h.getContent());
                case "SYSTEM" -> new SystemMessage(h.getContent());
                default -> new UserMessage(h.getContent());
            };
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        repository.deleteByConversationId(conversationId);
    }
}
