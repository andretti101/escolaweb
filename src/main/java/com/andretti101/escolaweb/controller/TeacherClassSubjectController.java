package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.TeacherClassSubjectRequestDTO;
import com.andretti101.escolaweb.dto.response.TeacherClassSubjectResponseDTO;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
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

import java.util.List;

@RestController
@RequestMapping("/api/teacher-class-subjects")
@RequiredArgsConstructor
public class TeacherClassSubjectController {

    private final TeacherClassSubjectService teacherClassSubjectService;

    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherClassSubjectResponseDTO> create(
            @Valid @RequestBody TeacherClassSubjectRequestDTO dto) {
        TeacherClassSubject created = teacherClassSubjectService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER')")
    public ResponseEntity<List<TeacherClassSubjectResponseDTO>> findAll() {
        return ResponseEntity.ok(
                teacherClassSubjectService.findAll().stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<TeacherClassSubjectResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherClassSubjectService.findById(id)));
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER')")
    public ResponseEntity<List<TeacherClassSubjectResponseDTO>> findByTeacher(@PathVariable Integer teacherId) {
        return ResponseEntity.ok(
                teacherClassSubjectService.findByTeacher(teacherId).stream().map(this::toResponse).toList()
        );
    }

    @GetMapping("/classroom/{classRoomId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<TeacherClassSubjectResponseDTO>> findByClassRoom(@PathVariable Integer classRoomId) {
        return ResponseEntity.ok(
                teacherClassSubjectService.findByClassRoom(classRoomId).stream().map(this::toResponse).toList()
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherClassSubjectResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody TeacherClassSubjectRequestDTO dto) {
        TeacherClassSubject updated = teacherClassSubjectService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        teacherClassSubjectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private TeacherClassSubject toEntity(TeacherClassSubjectRequestDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setId(dto.teacherId());

        ClassRoom classRoom = new ClassRoom();
        classRoom.setId(dto.classRoomId());

        Subject subject = new Subject();
        subject.setId(dto.subjectId());

        TeacherClassSubject tcs = new TeacherClassSubject();
        tcs.setTeacher(teacher);
        tcs.setClassRoom(classRoom);
        tcs.setSubject(subject);
        tcs.setMinAssessmentsPerPeriod(dto.minAssessmentsPerPeriod());
        tcs.setMaxAssessmentsPerPeriod(dto.maxAssessmentsPerPeriod());
        return tcs;
    }

    private TeacherClassSubjectResponseDTO toResponse(TeacherClassSubject tcs) {
        return new TeacherClassSubjectResponseDTO(
                tcs.getId(),
                tcs.getTeacher().getId(),
                tcs.getTeacher().getName(),
                tcs.getClassRoom().getId(),
                tcs.getClassRoom().getName(),
                tcs.getSubject().getId(),
                tcs.getSubject().getName(),
                tcs.getMinAssessmentsPerPeriod(),
                tcs.getMaxAssessmentsPerPeriod(),
                tcs.getCreatedAt()
        );
    }
}
