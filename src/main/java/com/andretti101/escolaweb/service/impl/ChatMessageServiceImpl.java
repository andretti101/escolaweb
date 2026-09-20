package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.response.ChatMessageResponseDTO;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ClassRoomRepository classRoomRepository;
    private final UserRepository userRepository;
    private final com.andretti101.escolaweb.repository.ChatReadReceiptRepository chatReadReceiptRepository;
    private final com.andretti101.escolaweb.repository.ChatHiddenMessageRepository chatHiddenMessageRepository;

    @Override
    @Transactional
    public ChatMessageResponseDTO saveMessage(Integer classroomId, Integer senderId, String content, Integer repliedToId) {
        ClassRoom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada. ID: " + classroomId));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado. ID: " + senderId));

        if (sender instanceof com.andretti101.escolaweb.model.entity.Student && 
            ((com.andretti101.escolaweb.model.entity.Student) sender).isChatBlocked()) {
            throw new IllegalArgumentException("Você foi bloqueado e não pode enviar mensagens neste chat.");
        }

        ChatMessage message = ChatMessage.builder()
                .classroom(classroom)
                .sender(sender)
                .content(content)
                .isEdited(false)
                .isDeleted(false)
                .deletedByAdmin(false)
                .build();

        if (repliedToId != null) {
            ChatMessage repliedToMessage = chatMessageRepository.findById(repliedToId)
                    .orElse(null);
            message.setRepliedTo(repliedToMessage);
        }

        ChatMessage savedMessage = chatMessageRepository.save(message);
        return mapToResponseDTO(savedMessage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageResponseDTO> getHistoryByClassroomId(Integer classroomId, Integer userId) {
        if (!classRoomRepository.existsById(classroomId)) {
            throw new EntityNotFoundException("Turma não encontrada. ID: " + classroomId);
        }
        List<ChatMessage> messages = chatMessageRepository.findByClassroom_IdOrderByTimestampAsc(classroomId);
        
        List<Integer> tempHiddenIds = new java.util.ArrayList<>();
        try {
            tempHiddenIds = chatHiddenMessageRepository.findByUserId(userId).stream()
                    .map(h -> h.getMessage().getId())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Erro ao buscar mensagens ocultas: " + e.getMessage());
        }
        
        final List<Integer> finalHiddenIds = tempHiddenIds;

        return messages.stream()
                .filter(m -> !finalHiddenIds.contains(m.getId()))
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessageResponseDTO editMessage(Integer messageId, Integer senderId, String newContent) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Mensagem não encontrada"));

        if (!message.getSender().getId().equals(senderId)) {
            throw new org.springframework.security.access.AccessDeniedException("Você só pode editar suas próprias mensagens.");
        }

        if (message.isDeleted()) {
            throw new IllegalArgumentException("Não é possível editar uma mensagem excluída.");
        }

        message.setContent(newContent);
        message.setEdited(true);

        ChatMessage updatedMessage = chatMessageRepository.save(message);
        return mapToResponseDTO(updatedMessage);
    }

    @Override
    @Transactional
    public ChatMessageResponseDTO deleteMessage(Integer messageId, Integer userId, boolean isAdmin) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Mensagem não encontrada"));

        boolean isAuthor = message.getSender().getId().equals(userId);

        if (!isAuthor && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("Você não tem permissão para excluir esta mensagem.");
        }

        message.setContent(""); // Limpa o texto original
        message.setDeleted(true);
        message.setDeletedByAdmin(!isAuthor && isAdmin);

        ChatMessage updatedMessage = chatMessageRepository.save(message);
        return mapToResponseDTO(updatedMessage);
    }

    private ChatMessageResponseDTO mapToResponseDTO(ChatMessage message) {
        ChatMessageResponseDTO dto = ChatMessageResponseDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .classroomId(message.getClassroom().getId())
                .isEdited(message.isEdited())
                .isDeleted(message.isDeleted())
                .deletedByAdmin(message.isDeletedByAdmin())
                .senderRole(message.getSender().getRole() != null ? message.getSender().getRole().name() : null)
                .build();
                
        if (message.getRepliedTo() != null) {
            dto.setRepliedToId(message.getRepliedTo().getId());
            dto.setRepliedToContent(message.getRepliedTo().getContent());
            dto.setRepliedToSenderName(message.getRepliedTo().getSender().getName());
        }
        
        return dto;
    }

    @Override
    @Transactional
    public void markAsRead(Integer userId, Integer classroomId, Integer messageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        ClassRoom classroom = classRoomRepository.findById(classroomId)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));

        com.andretti101.escolaweb.model.entity.ChatReadReceipt receipt = chatReadReceiptRepository
                .findByUserIdAndClassroomId(userId, classroomId)
                .orElse(com.andretti101.escolaweb.model.entity.ChatReadReceipt.builder()
                        .user(user)
                        .classroom(classroom)
                        .build());

        if (receipt.getLastReadMessageId() == null || messageId > receipt.getLastReadMessageId()) {
            receipt.setLastReadMessageId(messageId);
            receipt.setLastReadAt(java.time.LocalDateTime.now());
            chatReadReceiptRepository.save(receipt);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUnreadMessages(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
                
        // A simple logic: if any classroom has a message ID greater than the user's lastReadMessageId for that classroom
        // For students, they are linked to 1 classroom. For admins, they might see all.
        // Let's get the user's role to determine classrooms to check.
        List<ClassRoom> classroomsToCheck;
        if (user instanceof com.andretti101.escolaweb.model.entity.Student) {
            com.andretti101.escolaweb.model.entity.Student student = (com.andretti101.escolaweb.model.entity.Student) user;
            java.util.List<Integer> studentClassRoomIds = student.getEnrollments().stream()
                    .map(e -> e.getClassRoom().getId())
                    .collect(Collectors.toList());
            classroomsToCheck = classRoomRepository.findAll().stream()
                    .filter(c -> studentClassRoomIds.contains(c.getId()))
                    .collect(Collectors.toList());
        } else {
            return false; // Não notificar admins
        }

        for (ClassRoom classroom : classroomsToCheck) {
            Integer lastRead = getLastReadMessageId(userId, classroom.getId());
            
            long unreadCount = chatMessageRepository.countUnreadMessages(classroom.getId(), lastRead, userId);
            if (unreadCount > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getLastReadMessageId(Integer userId, Integer classroomId) {
        try {
            return chatReadReceiptRepository.findByUserIdAndClassroomId(userId, classroomId)
                    .map(com.andretti101.escolaweb.model.entity.ChatReadReceipt::getLastReadMessageId)
                    .orElse(0);
        } catch (Exception e) {
            System.err.println("Erro ao buscar read receipts: " + e.getMessage());
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getUsersToNotify(Integer classroomId, Integer senderId) {
        List<Integer> userIds = userRepository.findStudentIdsByClassroomId(classroomId);

        return userIds.stream()
                .filter(id -> !id.equals(senderId))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void hideMessage(Integer messageId, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (chatHiddenMessageRepository.findByUserIdAndMessageId(userId, messageId).isEmpty()) {
            chatHiddenMessageRepository.save(com.andretti101.escolaweb.model.entity.ChatHiddenMessage.builder()
                    .user(user)
                    .message(message)
                    .build());
        }
    }
}
