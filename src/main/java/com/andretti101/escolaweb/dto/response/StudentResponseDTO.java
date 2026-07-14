package com.andretti101.escolaweb.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponseDTO(
        Integer id,
        String name,
        String email,
        String ownPhone,
        String guardianPhone,
        String registrationNumber,
        LocalDate birthDate,
        boolean active,
        LocalDateTime createdAt
) {}
