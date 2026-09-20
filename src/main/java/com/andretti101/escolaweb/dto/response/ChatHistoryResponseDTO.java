package com.andretti101.escolaweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponseDTO {
    private List<ChatMessageResponseDTO> messages;
    private Integer lastReadMessageId;
    private Boolean isChatBlocked;
}
