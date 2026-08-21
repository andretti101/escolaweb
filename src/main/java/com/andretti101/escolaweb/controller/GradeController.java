package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.GradeLaunchRequestDTO;
import com.andretti101.escolaweb.dto.request.GradeUpdateRequestDTO;
import com.andretti101.escolaweb.dto.response.GradeResponseDTO;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.service.AssessmentService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.GradeService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final AuthenticatedUserService authenticatedUserService;
    private final AssessmentService assessmentService;
    private final TeacherClassSubjectService teacherClassSubjectService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<GradeResponseDTO> launch(@Valid @RequestBody GradeLaunchRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Assessment assessment = assessmentService.findById(dto.assessmentId());
            authenticatedUserService.enforceTeacherOwnership(assessment.getTeacherClassSubject());
        }
        Grade launched = gradeService.launch(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(launched));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<GradeResponseDTO>> findAll() {
        return ResponseEntity.ok(
                gradeService.findAll().stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<GradeResponseDTO> findById(@PathVariable Integer id) {
        Grade grade = gradeService.findById(id);
        authenticatedUserService.enforceStudentOwnership(grade.getStudent().getId());
        authenticatedUserService.enforceTeacherOwnership(grade.getAssessment().getTeacherClassSubject());
        return ResponseEntity.ok(toResponse(grade));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<List<GradeResponseDTO>> findByStudent(@PathVariable Integer studentId) {
        authenticatedUserService.enforceStudentOwnership(studentId);

        List<Grade> grades = gradeService.findByStudent(studentId);

        if (authenticatedUserService.isTeacher()) {
            Set<Integer> myTcsIds = getAuthenticatedTeacherTcsIds();
            grades = grades.stream()
                    .filter(g -> myTcsIds.contains(g.getAssessment().getTeacherClassSubject().getId()))
                    .toList();
        }

        return ResponseEntity.ok(grades.stream().map(this::toResponse).toList());
    }

    @GetMapping("/assessment/{assessmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<GradeResponseDTO>> findByAssessment(@PathVariable Integer assessmentId) {
        if (authenticatedUserService.isTeacher()) {
            Assessment assessment = assessmentService.findById(assessmentId);
            authenticatedUserService.enforceTeacherOwnership(assessment.getTeacherClassSubject());
        }
        return ResponseEntity.ok(
                gradeService.findByAssessment(assessmentId).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/average/student/{studentId}/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<BigDecimal> calculateAverage(
            @PathVariable Integer studentId,
            @PathVariable Integer tcsId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(tcsId);
            authenticatedUserService.enforceTeacherOwnership(tcs);
        }
        return ResponseEntity.ok(gradeService.calculateAverage(studentId, tcsId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<GradeResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody GradeUpdateRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Grade existing = gradeService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(
                    existing.getAssessment().getTeacherClassSubject());
        }
        Grade incoming = new Grade();
        incoming.setValue(dto.value());
        return ResponseEntity.ok(toResponse(gradeService.update(id, incoming)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        gradeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers

    private Set<Integer> getAuthenticatedTeacherTcsIds() {
        Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
        return teacherClassSubjectService.findByTeacher(teacher.getId())
                .stream().map(TeacherClassSubject::getId).collect(Collectors.toSet());
    }

    // ── Mapping

    private Grade toEntity(GradeLaunchRequestDTO dto) {
        Student student = new Student();
        student.setId(dto.studentId());

        Assessment assessment = new Assessment();
        assessment.setId(dto.assessmentId());

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setAssessment(assessment);
        grade.setValue(dto.value());
        return grade;
    }

    private GradeResponseDTO toResponse(Grade g) {
        return new GradeResponseDTO(
                g.getId(),
                g.getStudent().getId(),
                g.getStudent().getName(),
                g.getAssessment().getId(),
                g.getAssessment().getTitle(),
                g.getValue(),
                g.getStudentSituation(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}
