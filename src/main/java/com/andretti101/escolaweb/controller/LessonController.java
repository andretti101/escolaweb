package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.LessonRequestDTO;
import com.andretti101.escolaweb.dto.response.LessonResponseDTO;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<LessonResponseDTO> create(@Valid @RequestBody LessonRequestDTO dto) {
        Lesson created = lessonService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<LessonResponseDTO>> findAll() {
        return ResponseEntity.ok(
                lessonService.findAll().stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<LessonResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(lessonService.findById(id)));
    }

    @GetMapping("/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<LessonResponseDTO>> findByTeacherClassSubject(@PathVariable Integer tcsId) {
        return ResponseEntity.ok(
                lessonService.findByTeacherClassSubject(tcsId).stream().map(this::toResponse).toList()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<LessonResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody LessonRequestDTO dto) {
        Lesson updated = lessonService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private Lesson toEntity(LessonRequestDTO dto) {
        TeacherClassSubject tcs = new TeacherClassSubject();
        tcs.setId(dto.teacherClassSubjectId());

        Lesson lesson = new Lesson();
        lesson.setLessonDate(dto.lessonDate());
        lesson.setContent(dto.content());
        lesson.setNotes(dto.notes());
        lesson.setTeacherClassSubject(tcs);
        return lesson;
    }

    private LessonResponseDTO toResponse(Lesson l) {
        return new LessonResponseDTO(
                l.getId(),
                l.getLessonDate(),
                l.getContent(),
                l.getNotes(),
                l.getTeacherClassSubject().getId(),
                l.getTeacherClassSubject().getSubject().getName(),
                l.getTeacherClassSubject().getTeacher().getName(),
                l.getTeacherClassSubject().getClassRoom().getName(),
                l.getCreatedAt(),
                l.getUpdatedAt()
        );
    }
}
