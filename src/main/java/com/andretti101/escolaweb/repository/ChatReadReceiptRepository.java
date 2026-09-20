package com.andretti101.escolaweb.repository;

import com.andretti101.escolaweb.model.entity.ChatReadReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatReadReceiptRepository extends JpaRepository<ChatReadReceipt, Integer> {
    Optional<ChatReadReceipt> findByUserIdAndClassroomId(Integer userId, Integer classroomId);
}
