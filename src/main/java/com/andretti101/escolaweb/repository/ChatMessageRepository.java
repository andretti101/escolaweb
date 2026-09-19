package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    
    List<ChatMessage> findByClassroom_IdOrderByTimestampAsc(Integer classroomId);

}
