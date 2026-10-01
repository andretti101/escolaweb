package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.YearConclusionRequestDTO;

import java.util.Map;

public interface AcademicYearConclusionService {
    Map<String, Object> concludeYear(YearConclusionRequestDTO request);
    boolean canConcludeYear();
}
