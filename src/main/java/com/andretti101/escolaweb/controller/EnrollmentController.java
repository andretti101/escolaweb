package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.EnrollmentRequestDTO;
import com.andretti101.escolaweb.dto.response.EnrollmentResponseDTO;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.service.EnrollmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> create(@Valid @RequestBody EnrollmentRequestDTO dto) {
        Enrollment created = enrollmentService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<EnrollmentResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Enrollment> enrollments = activeOnly
                ? enrollmentService.findAllActive()
                : enrollmentService.findAll();
        return ResponseEntity.ok(enrollments.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<EnrollmentResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(enrollmentService.findById(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<List<EnrollmentResponseDTO>> findByStudent(@PathVariable Integer studentId) {
        return ResponseEntity.ok(
                enrollmentService.findByStudent(studentId).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/classroom/{classRoomId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER')")
    public ResponseEntity<List<EnrollmentResponseDTO>> findByClassRoom(@PathVariable Integer classRoomId) {
        return ResponseEntity.ok(
                enrollmentService.findByClassRoom(classRoomId).stream().map(this::toResponse).toList()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody EnrollmentRequestDTO dto) {
        Enrollment updated = enrollmentService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        enrollmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(enrollmentService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(enrollmentService.deactivate(id)));
    }

    // ── Mapping

    private Enrollment toEntity(EnrollmentRequestDTO dto) {
        Student student = new Student();
        student.setId(dto.studentId());

        ClassRoom classRoom = new ClassRoom();
        classRoom.setId(dto.classRoomId());

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassRoom(classRoom);
        enrollment.setEnrollmentDate(dto.enrollmentDate());
        return enrollment;
    }

    private EnrollmentResponseDTO toResponse(Enrollment e) {
        return new EnrollmentResponseDTO(
                e.getId(),
                e.getStudent().getId(),
                e.getStudent().getName(),
                e.getClassRoom().getId(),
                e.getClassRoom().getName(),
                e.getClassRoom().getAcademicYear().getYear(),
                e.getEnrollmentDate(),
                e.isActive(),
                e.getCreatedAt()
        );
    }
}
