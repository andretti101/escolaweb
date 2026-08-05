package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.TeacherRequestDTO;
import com.andretti101.escolaweb.dto.response.TeacherResponseDTO;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> create(@Valid @RequestBody TeacherRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(teacherService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<TeacherResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Teacher> teachers = activeOnly
                ? teacherService.findAllActive()
                : teacherService.findAll();
        return ResponseEntity.ok(teachers.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<TeacherResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody TeacherRequestDTO dto) {
        return ResponseEntity.ok(toResponse(teacherService.update(id, toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherService.deactivate(id)));
    }

    // ── Mapping

    private Teacher toEntity(TeacherRequestDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setName(dto.name());
        teacher.setEmail(dto.email());
        teacher.setPassword(dto.password()); // encoded by TeacherServiceImpl.create()
        return teacher;
    }

    private TeacherResponseDTO toResponse(Teacher t) {
        return new TeacherResponseDTO(
                t.getId(),
                t.getName(),
                t.getEmail(),
                t.isActive(),
                t.getCreatedAt());
    }
}
