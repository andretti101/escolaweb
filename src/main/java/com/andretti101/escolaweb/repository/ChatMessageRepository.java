package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    List<ChatMessage> findByClassroom_IdOrderByTimestampAsc(Integer classroomId);
    
    java.util.Optional<ChatMessage> findFirstByClassroomOrderByTimestampDesc(com.andretti101.escolaweb.model.entity.ClassRoom classroom);
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.classroom.id = :classroomId AND m.id > :lastReadId AND m.sender.id != :userId AND m.isDeleted = false")
    long countUnreadMessages(@org.springframework.data.repository.query.Param("classroomId") Integer classroomId, @org.springframework.data.repository.query.Param("lastReadId") Integer lastReadId, @org.springframework.data.repository.query.Param("userId") Integer userId);
}
