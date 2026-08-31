package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AcademicYearRequestDTO;
import com.andretti101.escolaweb.dto.response.AcademicYearResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicYearResponseDTO> create(@Valid @RequestBody AcademicYearRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(academicYearService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<AcademicYearResponseDTO>> findAll() {
        return ResponseEntity.ok(
                academicYearService.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AcademicYearResponseDTO> findActive() {
        return ResponseEntity.ok(toResponse(academicYearService.findActive()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AcademicYearResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicYearService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicYearResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody AcademicYearRequestDTO dto) {
        return ResponseEntity.ok(toResponse(academicYearService.update(id, toEntity(dto))));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicYearResponseDTO> setActive(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicYearService.setActive(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        academicYearService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private AcademicYear toEntity(AcademicYearRequestDTO dto) {
        AcademicYear year = new AcademicYear();
        year.setYear(dto.year());
        year.setStartDate(dto.startDate());
        year.setEndDate(dto.endDate());
        return year;
    }

    private AcademicYearResponseDTO toResponse(AcademicYear y) {
        return new AcademicYearResponseDTO(
                y.getId(),
                y.getYear(),
                y.getStartDate(),
                y.getEndDate(),
                y.isActive(),
                y.getCreatedAt());
    }
}
