package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.ChatMessage;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.ChatMessageRepository;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.ChatMessageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ClassRoomRepository classRoomRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatMessage saveMessage(Integer classroomId, Integer senderId, String content) {
        ClassRoom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada. ID: " + classroomId));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado. ID: " + senderId));

        ChatMessage message = ChatMessage.builder()
                .classroom(classroom)
                .sender(sender)
                .content(content)
                .build();

        return chatMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getHistoryByClassroomId(Integer classroomId) {
        if (!classRoomRepository.existsById(classroomId)) {
            throw new EntityNotFoundException("Turma não encontrada. ID: " + classroomId);
        }
        return chatMessageRepository.findByClassroom_IdOrderByTimestampAsc(classroomId);
    }
}
