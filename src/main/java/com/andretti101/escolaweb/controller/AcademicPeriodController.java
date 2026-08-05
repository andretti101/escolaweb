package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AcademicPeriodRequestDTO;
import com.andretti101.escolaweb.dto.response.AcademicPeriodResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<AcademicPeriodResponseDTO> create(@Valid @RequestBody AcademicPeriodRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(academicPeriodService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> findAll() {
        return ResponseEntity.ok(
                academicPeriodService.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AcademicPeriodResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicPeriodService.findById(id)));
    }

    @GetMapping("/academic-year/{yearId}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> findByAcademicYear(@PathVariable Integer yearId) {
        return ResponseEntity.ok(
                academicPeriodService.findByAcademicYear(yearId).stream().map(this::toResponse).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<AcademicPeriodResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody AcademicPeriodRequestDTO dto) {
        return ResponseEntity.ok(toResponse(academicPeriodService.update(id, toEntity(dto))));
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicPeriodResponseDTO> close(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicPeriodService.close(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        academicPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private AcademicPeriod toEntity(AcademicPeriodRequestDTO dto) {
        AcademicYear year = new AcademicYear();
        year.setId(dto.academicYearId());

        AcademicPeriod period = new AcademicPeriod();
        period.setName(dto.name());
        period.setStartDate(dto.startDate());
        period.setEndDate(dto.endDate());
        period.setAcademicYear(year);
        return period;
    }

    private AcademicPeriodResponseDTO toResponse(AcademicPeriod p) {
        return new AcademicPeriodResponseDTO(
                p.getId(),
                p.getName(),
                p.getStartDate(),
                p.getEndDate(),
                p.isClosed(),
                p.getAcademicYear().getId(),
                p.getAcademicYear().getYear(),
                p.getCreatedAt());
    }
}
