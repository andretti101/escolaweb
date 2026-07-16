package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.StudentRequestDTO;
import com.andretti101.escolaweb.dto.response.StudentResponseDTO;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.service.StudentService;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> create(@Valid @RequestBody StudentRequestDTO dto) {
        Student created = studentService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<StudentResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Student> students = activeOnly
                ? studentService.findAllActive()
                : studentService.findAll();
        return ResponseEntity.ok(students.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<StudentResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(studentService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody StudentRequestDTO dto) {
        Student updated = studentService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(studentService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(studentService.deactivate(id)));
    }

    // ── Mapping

    private Student toEntity(StudentRequestDTO dto) {
        Student student = new Student();
        student.setName(dto.name());
        student.setEmail(dto.email());
        student.setPassword(dto.password());
        student.setOwnPhone(dto.ownPhone());
        student.setGuardianPhone(dto.guardianPhone());
        student.setRegistrationNumber(dto.registrationNumber());
        student.setBirthDate(dto.birthDate());
        return student;
    }

    private StudentResponseDTO toResponse(Student s) {
        return new StudentResponseDTO(
                s.getId(),
                s.getName(),
                s.getEmail(),
                s.getOwnPhone(),
                s.getGuardianPhone(),
                s.getRegistrationNumber(),
                s.getBirthDate(),
                s.isActive(),
                s.getCreatedAt()
        );
    }
}
