package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AssessmentRequestDTO;
import com.andretti101.escolaweb.dto.response.AssessmentResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.service.AssessmentService;
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
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AssessmentResponseDTO> create(@Valid @RequestBody AssessmentRequestDTO dto) {
        Assessment created = assessmentService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AssessmentResponseDTO>> findAll() {
        return ResponseEntity.ok(
                assessmentService.findAll().stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<AssessmentResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(assessmentService.findById(id)));
    }

    @GetMapping("/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AssessmentResponseDTO>> findByTeacherClassSubject(@PathVariable Integer tcsId) {
        return ResponseEntity.ok(
                assessmentService.findByTeacherClassSubject(tcsId).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/period/{periodId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AssessmentResponseDTO>> findByPeriod(@PathVariable Integer periodId) {
        return ResponseEntity.ok(
                assessmentService.findByPeriod(periodId).stream().map(this::toResponse).toList()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AssessmentResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody AssessmentRequestDTO dto) {
        Assessment updated = assessmentService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        assessmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private Assessment toEntity(AssessmentRequestDTO dto) {
        TeacherClassSubject tcs = new TeacherClassSubject();
        tcs.setId(dto.teacherClassSubjectId());

        AcademicPeriod period = new AcademicPeriod();
        period.setId(dto.periodId());

        Assessment assessment = new Assessment();
        assessment.setTitle(dto.title());
        assessment.setDescription(dto.description());
        assessment.setDate(dto.date());
        assessment.setAssessmentType(dto.assessmentType());
        assessment.setTeacherClassSubject(tcs);
        assessment.setPeriod(period);
        return assessment;
    }

    private AssessmentResponseDTO toResponse(Assessment a) {
        return new AssessmentResponseDTO(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getDate(),
                a.getAssessmentType(),
                a.getTeacherClassSubject().getId(),
                a.getTeacherClassSubject().getSubject().getName(),
                a.getTeacherClassSubject().getTeacher().getName(),
                a.getPeriod().getId(),
                a.getPeriod().getName(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
