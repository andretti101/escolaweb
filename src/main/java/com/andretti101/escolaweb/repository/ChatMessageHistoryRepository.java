package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ChatMessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageHistoryRepository extends JpaRepository<ChatMessageHistory, Long> {
    List<ChatMessageHistory> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    void deleteByConversationId(String conversationId);
}
