package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.ChatMessageResponseDTO;

import java.util.List;

public interface ChatMessageService {
    
    ChatMessageResponseDTO saveMessage(Integer classroomId, Integer senderId, String content, Integer repliedToId);
    
    ChatMessageResponseDTO editMessage(Integer messageId, Integer senderId, String newContent);
    ChatMessageResponseDTO deleteMessage(Integer messageId, Integer userId, boolean isAdmin);

    void markAsRead(Integer userId, Integer classroomId, Integer messageId);
    boolean hasUnreadMessages(Integer userId);
    Integer getLastReadMessageId(Integer userId, Integer classroomId);
    List<Integer> getUsersToNotify(Integer classroomId, Integer senderId);
    void hideMessage(Integer messageId, Integer userId);
    List<ChatMessageResponseDTO> getHistoryByClassroomId(Integer classroomId, Integer userId);
}
