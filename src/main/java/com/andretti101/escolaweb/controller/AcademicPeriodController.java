package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AcademicPeriodRequestDTO;
import com.andretti101.escolaweb.dto.request.CreateDivisionRequestDTO;
import com.andretti101.escolaweb.dto.response.AcademicPeriodResponseDTO;
import com.andretti101.escolaweb.dto.response.DivisionResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;
    private final AcademicYearService academicYearService;
    private final ClassRoomRepository classRoomRepository;

    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
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
    public ResponseEntity<List<AcademicPeriodResponseDTO>> findByAcademicYear(
            @PathVariable Integer yearId,
            @RequestParam(required = false) AcademicPeriodType type) {
        List<AcademicPeriod> list = (type != null)
                ? academicPeriodService.findByAcademicYearAndType(yearId, type)
                : academicPeriodService.findByAcademicYear(yearId);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @GetMapping("/divisions")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<DivisionResponseDTO>> getDivisions(@RequestParam Integer yearId) {
        AcademicYear year = academicYearService.findById(yearId);
        Map<AcademicPeriodType, List<AcademicPeriod>> divisions = academicPeriodService.findDivisions(yearId);
        List<DivisionResponseDTO> result = new ArrayList<>();

        for (Map.Entry<AcademicPeriodType, List<AcademicPeriod>> entry : divisions.entrySet()) {
            AcademicPeriodType type = entry.getKey();
            List<AcademicPeriodResponseDTO> periods = entry.getValue().stream()
                    .map(this::toResponse).toList();
            boolean hasActiveClasses = classRoomRepository.existsByAcademicYearAndPeriodTypeAndActiveTrue(year, type);
            result.add(new DivisionResponseDTO(type, type.getLabel(), type.getPeriodsCount(), hasActiveClasses, periods));
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/divisions")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> createDivision(@Valid @RequestBody CreateDivisionRequestDTO dto) {
        List<AcademicPeriod> periods = academicPeriodService.createDivision(dto.academicYearId(), dto.periodType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(periods.stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/divisions")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> deleteDivision(
            @RequestParam Integer yearId,
            @RequestParam AcademicPeriodType type) {
        academicPeriodService.deleteDivision(yearId, type);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
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
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        academicPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/open-next/{yearId}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicPeriodResponseDTO> openNextPeriod(
            @PathVariable Integer yearId,
            @RequestParam(required = false) AcademicPeriodType type) {
        return ResponseEntity.ok(toResponse(academicPeriodService.openNextPeriod(yearId, type)));
    }

    // ── Mapping

    private AcademicPeriod toEntity(AcademicPeriodRequestDTO dto) {
        AcademicYear year = new AcademicYear();
        year.setId(dto.academicYearId());

        AcademicPeriod period = new AcademicPeriod();
        period.setName(dto.name());
        period.setPeriodType(dto.periodType());
        period.setStartDate(dto.startDate());
        period.setEndDate(dto.endDate());
        period.setAcademicYear(year);
        return period;
    }

    private AcademicPeriodResponseDTO toResponse(AcademicPeriod p) {
        return new AcademicPeriodResponseDTO(
                p.getId(),
                p.getName(),
                p.getPeriodType(),
                p.getPeriodType() != null ? p.getPeriodType().getLabel() : null,
                p.getStartDate(),
                p.getEndDate(),
                p.isClosed(),
                p.getAcademicYear() != null ? p.getAcademicYear().getId() : null,
                p.getAcademicYear() != null ? p.getAcademicYear().getYear() : null,
                p.getCreatedAt());
    }
}
