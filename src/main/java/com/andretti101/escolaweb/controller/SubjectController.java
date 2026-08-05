package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.SubjectRequestDTO;
import com.andretti101.escolaweb.dto.response.SubjectResponseDTO;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> create(@Valid @RequestBody SubjectRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(subjectService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<SubjectResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Subject> subjects = activeOnly
                ? subjectService.findAllActive()
                : subjectService.findAll();
        return ResponseEntity.ok(subjects.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<SubjectResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(subjectService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody SubjectRequestDTO dto) {
        return ResponseEntity.ok(toResponse(subjectService.update(id, toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(subjectService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(subjectService.deactivate(id)));
    }

    // ── Mapping

    private Subject toEntity(SubjectRequestDTO dto) {
        Subject subject = new Subject();
        subject.setName(dto.name());
        subject.setDescription(dto.description());
        return subject;
    }

    private SubjectResponseDTO toResponse(Subject s) {
        return new SubjectResponseDTO(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.isActive(),
                s.getCreatedAt());
    }
}
