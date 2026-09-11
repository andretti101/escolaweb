package com.andretti101.escolaweb.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record StudentAttendanceReportDTO(
        Integer studentId,
        BigDecimal generalFrequency,
        Map<String, Integer> absencesPerSubject,
        Map<String, BigDecimal> frequencyPerSubject
) {}
