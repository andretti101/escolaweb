package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    
    ChatMessage saveMessage(Integer classroomId, Integer senderId, String content);
    
    List<ChatMessage> getHistoryByClassroomId(Integer classroomId);
}
