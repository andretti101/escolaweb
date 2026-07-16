package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.SchoolSettingsRequestDTO;
import com.andretti101.escolaweb.dto.response.SchoolSettingsResponseDTO;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.service.SchoolSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/school-settings")
@RequiredArgsConstructor
public class SchoolSettingsController {

    private final SchoolSettingsService schoolSettingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SchoolSettingsResponseDTO> getSettings() {
        return ResponseEntity.ok(toResponse(schoolSettingsService.findSettings()));
    }

    @PutMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<SchoolSettingsResponseDTO> update(@Valid @RequestBody SchoolSettingsRequestDTO dto) {
        SchoolSettings updated = schoolSettingsService.update(toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    // ── Mapping

    private SchoolSettings toEntity(SchoolSettingsRequestDTO dto) {
        SchoolSettings settings = new SchoolSettings();
        settings.setSchoolName(dto.schoolName());
        settings.setAddress(dto.address());
        settings.setPhone(dto.phone());
        settings.setEmail(dto.email());
        settings.setMinimumGrade(dto.minimumGrade());
        settings.setMinimumAttendance(dto.minimumAttendance());
        settings.setPeriodType(dto.periodType());
        return settings;
    }

    private SchoolSettingsResponseDTO toResponse(SchoolSettings s) {
        return new SchoolSettingsResponseDTO(
                s.getId(),
                s.getSchoolName(),
                s.getAddress(),
                s.getPhone(),
                s.getEmail(),
                s.getMinimumGrade(),
                s.getMinimumAttendance(),
                s.getPeriodType(),
                s.getCreatedAt()
        );
    }
}
