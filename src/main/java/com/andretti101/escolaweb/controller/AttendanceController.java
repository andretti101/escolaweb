package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AttendanceRequestDTO;
import com.andretti101.escolaweb.dto.response.AttendanceResponseDTO;
import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.service.AttendanceService;
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

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AttendanceResponseDTO> register(@Valid @RequestBody AttendanceRequestDTO dto) {
        Attendance registered = attendanceService.register(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(registered));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AttendanceResponseDTO>> findAll() {
        return ResponseEntity.ok(
                attendanceService.findAll().stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<AttendanceResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(attendanceService.findById(id)));
    }

    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AttendanceResponseDTO>> findByLesson(@PathVariable Integer lessonId) {
        return ResponseEntity.ok(
                attendanceService.findByLesson(lessonId).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<List<AttendanceResponseDTO>> findByStudent(@PathVariable Integer studentId) {
        return ResponseEntity.ok(
                attendanceService.findByStudent(studentId).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/frequency/student/{studentId}/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<BigDecimal> calculateFrequency(
            @PathVariable Integer studentId,
            @PathVariable Integer tcsId) {
        return ResponseEntity.ok(attendanceService.calculateFrequency(studentId, tcsId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AttendanceResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody AttendanceRequestDTO dto) {
        Attendance updated = attendanceService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        attendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private Attendance toEntity(AttendanceRequestDTO dto) {
        Student student = new Student();
        student.setId(dto.studentId());

        Lesson lesson = new Lesson();
        lesson.setId(dto.lessonId());

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setLesson(lesson);
        attendance.setStatus(dto.status());
        attendance.setNotes(dto.notes());
        return attendance;
    }

    private AttendanceResponseDTO toResponse(Attendance a) {
        return new AttendanceResponseDTO(
                a.getId(),
                a.getStudent().getId(),
                a.getStudent().getName(),
                a.getLesson().getId(),
                a.getLesson().getLessonDate(),
                a.getStatus(),
                a.getNotes(),
                a.getCreatedAt()
        );
    }
}
