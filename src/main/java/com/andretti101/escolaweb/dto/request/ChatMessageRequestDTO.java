package com.andretti101.escolaweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDTO {
    
    @NotBlank(message = "O conteúdo da mensagem não pode ser vazio.")
    private String content;

}
