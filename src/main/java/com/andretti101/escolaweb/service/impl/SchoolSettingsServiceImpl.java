package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.repository.SchoolSettingsRepository;
import com.andretti101.escolaweb.service.SchoolSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SchoolSettingsServiceImpl implements SchoolSettingsService {

    private final SchoolSettingsRepository schoolSettingsRepository;

    @Override
    @Transactional
    public SchoolSettings findSettings() {
        return schoolSettingsRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaults);
    }

    @Override
    @Transactional
    public SchoolSettings update(SchoolSettings incoming) {
        SchoolSettings existing = findSettings();

        if (incoming.getSchoolName() != null) {
            existing.setSchoolName(incoming.getSchoolName());
        }
        if (incoming.getAddress() != null) {
            existing.setAddress(incoming.getAddress());
        }
        if (incoming.getPhone() != null) {
            existing.setPhone(incoming.getPhone());
        }
        if (incoming.getEmail() != null) {
            existing.setEmail(incoming.getEmail());
        }
        if (incoming.getMinimumGrade() != null) {
            validateGradeRange(incoming.getMinimumGrade());
            existing.setMinimumGrade(incoming.getMinimumGrade());
        }
        if (incoming.getMinimumAttendance() != null) {
            validateAttendanceRange(incoming.getMinimumAttendance());
            existing.setMinimumAttendance(incoming.getMinimumAttendance());
        }
        if (incoming.getPeriodType() != null) {
            existing.setPeriodType(incoming.getPeriodType());
        }

        return existing;
    }

    private SchoolSettings createDefaults() {
        SchoolSettings defaults = SchoolSettings.builder()
                .schoolName("Escola")
                .minimumGrade(new BigDecimal("6.0"))
                .minimumAttendance(new BigDecimal("75.00"))
                .periodType(AcademicPeriodType.TRIMESTER)
                .build();
        return schoolSettingsRepository.save(defaults);
    }

    private void validateGradeRange(BigDecimal grade) {
        if (grade.compareTo(BigDecimal.ZERO) < 0 || grade.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Minimum grade must be between 0 and 10.");
        }
    }

    private void validateAttendanceRange(BigDecimal attendance) {
        if (attendance.compareTo(BigDecimal.ZERO) < 0 || attendance.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Minimum attendance must be between 0 and 100.");
        }
    }
}
