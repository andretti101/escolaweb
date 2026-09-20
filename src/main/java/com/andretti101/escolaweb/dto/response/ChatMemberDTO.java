package com.andretti101.escolaweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMemberDTO {
    private Integer id;
    private String name;
    private String registrationNumber; // Only for admins
    private Boolean isBlocked; // Only for admins
}
