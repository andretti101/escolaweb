package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ChatHiddenMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatHiddenMessageRepository extends JpaRepository<ChatHiddenMessage, Integer> {
    List<ChatHiddenMessage> findByUserId(Integer userId);
    Optional<ChatHiddenMessage> findByUserIdAndMessageId(Integer userId, Integer messageId);
}
