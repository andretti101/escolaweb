package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ClassRoomRequestDTO;
import com.andretti101.escolaweb.dto.response.ClassRoomResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.service.ClassRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
public class ClassRoomController {

    private final ClassRoomService classRoomService;

    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> create(@Valid @RequestBody ClassRoomRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(classRoomService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ClassRoomResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<ClassRoom> classRooms = activeOnly
                ? classRoomService.findAllActive()
                : classRoomService.findAll();
        return ResponseEntity.ok(classRooms.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ClassRoomResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(classRoomService.findById(id)));
    }

    @GetMapping("/academic-year/{yearId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ClassRoomResponseDTO>> findByAcademicYear(@PathVariable Integer yearId) {
        return ResponseEntity.ok(
                classRoomService.findByAcademicYear(yearId).stream().map(this::toResponse).toList());
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
        classRoom.setCreationYear(dto.creationYear());
        classRoom.setAcademicYear(year);
        return classRoom;
    }

    private ClassRoomResponseDTO toResponse(ClassRoom c) {
        return new ClassRoomResponseDTO(
                c.getId(),
                c.getName(),
                c.getShift(),
                c.getCreationYear(),
                c.getAcademicYear().getId(),
                c.getAcademicYear().getYear(),
                c.isActive(),
                c.getCreatedAt());
    }
}
