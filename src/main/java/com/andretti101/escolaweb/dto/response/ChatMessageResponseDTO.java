package com.andretti101.escolaweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponseDTO {

    private Integer id;
    private String content;
    private LocalDateTime timestamp;
    private Integer senderId;
    private String senderName;
    private Integer classroomId;

}
