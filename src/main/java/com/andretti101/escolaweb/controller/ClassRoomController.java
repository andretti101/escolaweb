package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ClassRoomRequestDTO;
import com.andretti101.escolaweb.dto.response.AcademicPeriodResponseDTO;
import com.andretti101.escolaweb.dto.response.ClassRoomResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.service.ClassRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
public class ClassRoomController {

    private final ClassRoomService classRoomService;
    private final AcademicPeriodRepository academicPeriodRepository;

    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> create(@Valid @RequestBody ClassRoomRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(classRoomService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<org.springframework.data.domain.Page<ClassRoomResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "true") boolean activeOnly,
            @RequestParam(required = false) Integer academicYearId,
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<ClassRoom> page;
        if (academicYearId != null) {
            page = activeOnly
                    ? classRoomService.findByAcademicYearAndActiveTrue(academicYearId, pageable)
                    : classRoomService.findByAcademicYear(academicYearId, pageable);
        } else {
            page = activeOnly
                    ? classRoomService.findAllActive(pageable)
                    : classRoomService.findAll(pageable);
        }
        List<ClassRoomResponseDTO> dtoList = toResponseList(page.getContent());
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ClassRoomResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(classRoomService.findById(id)));
    }

    @GetMapping("/academic-year/{yearId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ClassRoomResponseDTO>> findByAcademicYear(@PathVariable Integer yearId) {
        return ResponseEntity.ok(toResponseList(classRoomService.findByAcademicYear(yearId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody ClassRoomRequestDTO dto) {
        return ResponseEntity.ok(toResponse(classRoomService.update(id, toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        classRoomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(classRoomService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(classRoomService.deactivate(id)));
    }

    // ── Mapping

    private ClassRoom toEntity(ClassRoomRequestDTO dto) {
        AcademicYear year = new AcademicYear();
        year.setId(dto.academicYearId());

        ClassRoom classRoom = new ClassRoom();
        classRoom.setName(dto.name());
        classRoom.setShift(dto.shift());
        classRoom.setPeriodType(dto.periodType());
        classRoom.setSchoolGrade(dto.schoolGrade());
        classRoom.setAcademicYear(year);
        return classRoom;
    }

    private List<ClassRoomResponseDTO> toResponseList(List<ClassRoom> classRooms) {
        if (classRooms == null || classRooms.isEmpty()) {
            return List.of();
        }
        Set<AcademicYear> years = classRooms.stream()
                .map(ClassRoom::getAcademicYear)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, List<AcademicPeriodResponseDTO>> periodsByYearAndType = years.isEmpty()
                ? Map.of()
                : academicPeriodRepository.findByAcademicYearInOrderByIdAsc(years).stream()
                        .map(this::toPeriodResponse)
                        .collect(Collectors.groupingBy(p -> p.academicYearId() + "_" + (p.periodType() != null ? p.periodType().name() : "")));

        return classRooms.stream().map(c -> {
            String key = (c.getAcademicYear() != null ? c.getAcademicYear().getId() : null)
                    + "_" + (c.getPeriodType() != null ? c.getPeriodType().name() : "");
            List<AcademicPeriodResponseDTO> periods = periodsByYearAndType.getOrDefault(key, List.of());
            return toResponseWithPeriods(c, periods);
        }).toList();
    }

    private ClassRoomResponseDTO toResponse(ClassRoom c) {
        if (c == null) {
            return null;
        }
        return toResponseList(List.of(c)).get(0);
    }

    private ClassRoomResponseDTO toResponseWithPeriods(ClassRoom c, List<AcademicPeriodResponseDTO> periods) {
        return new ClassRoomResponseDTO(
                c.getId(),
                c.getName(),
                c.getShift(),
                c.getSchoolGrade(),
                c.getSchoolGrade() != null ? c.getSchoolGrade().getLabel() : null,
                c.getPeriodType(),
                c.getPeriodType() != null ? c.getPeriodType().getLabel() : null,
                c.getAcademicYear() != null ? c.getAcademicYear().getId() : null,
                c.getAcademicYear() != null ? c.getAcademicYear().getYear() : null,
                c.isActive(),
                c.getCreatedAt(),
                periods);
    }

    private AcademicPeriodResponseDTO toPeriodResponse(AcademicPeriod p) {
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
