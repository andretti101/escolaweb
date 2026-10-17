package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    
    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    Optional<ChatMessage> findById(Integer id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    List<ChatMessage> findByClassroom_IdAndArchivedFalseOrderByTimestampAsc(Integer classroomId);
    
    Optional<ChatMessage> findFirstByClassroomAndArchivedFalseOrderByTimestampDesc(com.andretti101.escolaweb.model.entity.ClassRoom classroom);
    
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.classroom.id = :classroomId AND m.id > :lastReadId AND m.sender.id != :userId AND m.isDeleted = false AND m.archived = false")
    long countUnreadMessages(@Param("classroomId") Integer classroomId, @Param("lastReadId") Integer lastReadId, @Param("userId") Integer userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    List<ChatMessage> findByIsGlobalTeacherChatTrueAndArchivedFalseOrderByTimestampAsc();

    Optional<ChatMessage> findFirstByIsGlobalTeacherChatTrueAndArchivedFalseOrderByTimestampDesc();

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.isGlobalTeacherChat = true AND m.id > :lastReadId AND m.sender.id != :userId AND m.isDeleted = false AND m.archived = false")
    long countUnreadGlobalTeacherMessages(@Param("lastReadId") Integer lastReadId, @Param("userId") Integer userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    List<ChatMessage> findTop50ByClassroom_IdAndArchivedFalseOrderByTimestampDesc(Integer classroomId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    List<ChatMessage> findTop50ByIsGlobalTeacherChatTrueAndArchivedFalseOrderByTimestampDesc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    List<ChatMessage> findByClassroom_IdAndArchivedTrueOrderByTimestampAsc(Integer classroomId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    List<ChatMessage> findByIsGlobalTeacherChatTrueAndArchivedTrueOrderByTimestampAsc();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"sender", "repliedTo", "repliedTo.sender"})
    @Query("SELECT m FROM ChatMessage m WHERE m.isGlobalTeacherChat = true AND m.archived = true AND " +
           "(:year IS NULL OR YEAR(m.timestamp) = :year) ORDER BY m.timestamp ASC")
    List<ChatMessage> findArchivedGlobalTeacherHistory(@Param("year") Integer year);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessage m SET m.archived = true WHERE m.archived = false AND m.isGlobalTeacherChat = false")
    int archiveAll();
}
