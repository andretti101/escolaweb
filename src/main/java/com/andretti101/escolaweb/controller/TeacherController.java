package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.TeacherPatchRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherUpdateRequestDTO;
import com.andretti101.escolaweb.dto.response.SubjectResponseDTO;
import com.andretti101.escolaweb.dto.response.TeacherResponseDTO;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ResponseEntity<org.springframework.data.domain.Page<TeacherResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Teacher> page = activeOnly
                ? teacherService.findAllActive(pageable)
                : teacherService.findAll(pageable);
        List<TeacherResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<TeacherResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody TeacherUpdateRequestDTO dto) {
        return ResponseEntity.ok(toResponse(teacherService.update(id, toEntity(dto))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> patch(
            @PathVariable Integer id, @Valid @RequestBody TeacherPatchRequestDTO dto) {
        return ResponseEntity.ok(toResponse(teacherService.patch(id, dto)));
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
        teacher.setSubjects(mapSubjectStubs(dto.subjectIds(), false));
        return teacher;
    }

    private Teacher toEntity(TeacherUpdateRequestDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setName(dto.name());
        teacher.setEmail(dto.email());
        teacher.setPassword(dto.password());
        teacher.setSubjects(mapSubjectStubs(dto.subjectIds(), true));
        return teacher;
    }

    private Set<Subject> mapSubjectStubs(List<Integer> subjectIds, boolean preserveNull) {
        if (subjectIds == null) {
            return preserveNull ? null : new LinkedHashSet<>();
        }
        return subjectIds.stream()
                .filter(Objects::nonNull)
                .map(id -> {
                    Subject s = new Subject();
                    s.setId(id);
                    return s;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private TeacherResponseDTO toResponse(Teacher t) {
        List<SubjectResponseDTO> subjects = t.getSubjects() == null
                ? List.of()
                : t.getSubjects().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(
                                (Subject s) -> s.getName() != null ? s.getName().toLowerCase() : "",
                                Comparator.naturalOrder()))
                        .map(s -> new SubjectResponseDTO(
                                s.getId(),
                                s.getName(),
                                s.getDescription(),
                                s.isActive(),
                                s.getCreatedAt()))
                        .toList();

        return new TeacherResponseDTO(
                t.getId(),
                t.getName(),
                t.getEmail(),
                t.isActive(),
                t.getCreatedAt(),
                subjects);
    }
}
