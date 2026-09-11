package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AttendanceRequestDTO;
import com.andretti101.escolaweb.dto.response.AttendanceResponseDTO;
import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AttendanceService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.LessonService;
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
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthenticatedUserService authenticatedUserService;
    private final LessonService lessonService;
    private final TeacherClassSubjectService teacherClassSubjectService;

    private final AcademicPeriodService academicPeriodService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AttendanceResponseDTO> register(@Valid @RequestBody AttendanceRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Lesson lesson = lessonService.findById(dto.lessonId());
            authenticatedUserService.enforceTeacherOwnership(lesson.getTeacherClassSubject());
        }
        Attendance registered = attendanceService.register(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(registered));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AttendanceResponseDTO>> findAll() {
        List<Attendance> attendances = attendanceService.findAll();

        if (authenticatedUserService.isTeacher()) {
            Set<Integer> myTcsIds = getAuthenticatedTeacherTcsIds();
            attendances = attendances.stream()
                    .filter(a -> myTcsIds.contains(a.getLesson().getTeacherClassSubject().getId()))
                    .toList();
        }

        final var cachedPeriods = academicPeriodService.findAll();
        return ResponseEntity.ok(attendances.stream().map(a -> toResponse(a, cachedPeriods)).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<AttendanceResponseDTO> findById(@PathVariable Integer id) {
        Attendance attendance = attendanceService.findById(id);
        authenticatedUserService.enforceStudentOwnership(attendance.getStudent().getId());
        authenticatedUserService.enforceTeacherOwnership(attendance.getLesson().getTeacherClassSubject());
        return ResponseEntity.ok(toResponse(attendance));
    }

    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AttendanceResponseDTO>> findByLesson(@PathVariable Integer lessonId) {
        if (authenticatedUserService.isTeacher()) {
            Lesson lesson = lessonService.findById(lessonId);
            authenticatedUserService.enforceTeacherOwnership(lesson.getTeacherClassSubject());
        }
        final var cachedPeriods = academicPeriodService.findAll();
        return ResponseEntity.ok(
                attendanceService.findByLesson(lessonId).stream().map(a -> toResponse(a, cachedPeriods)).toList()
        );
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<List<AttendanceResponseDTO>> findByStudent(@PathVariable Integer studentId) {
        authenticatedUserService.enforceStudentOwnership(studentId);

        List<Attendance> attendances = attendanceService.findByStudent(studentId);

        if (authenticatedUserService.isTeacher()) {
            Set<Integer> myTcsIds = getAuthenticatedTeacherTcsIds();
            attendances = attendances.stream()
                    .filter(a -> myTcsIds.contains(a.getLesson().getTeacherClassSubject().getId()))
                    .toList();
        }

        final var cachedPeriods = academicPeriodService.findAll();
        return ResponseEntity.ok(attendances.stream().map(a -> toResponse(a, cachedPeriods)).toList());
    }

    @GetMapping("/frequency/student/{studentId}/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<BigDecimal> calculateFrequency(
            @PathVariable Integer studentId,
            @PathVariable Integer tcsId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(tcsId);
            authenticatedUserService.enforceTeacherOwnership(tcs);
        }
        return ResponseEntity.ok(attendanceService.calculateFrequency(studentId, tcsId));
    }

    @GetMapping("/report/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.StudentAttendanceReportDTO> getStudentAttendanceReport(
            @PathVariable Integer studentId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        return ResponseEntity.ok(attendanceService.getStudentAttendanceReport(studentId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AttendanceResponseDTO> update(
            @PathVariable Integer id,
            @Valid @RequestBody AttendanceRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Attendance existing = attendanceService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(
                    existing.getLesson().getTeacherClassSubject());
        }
        Attendance updated = attendanceService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (authenticatedUserService.isTeacher()) {
            Attendance existing = attendanceService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(
                    existing.getLesson().getTeacherClassSubject());
        }
        attendanceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers

    private Set<Integer> getAuthenticatedTeacherTcsIds() {
        Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
        return teacherClassSubjectService.findByTeacher(teacher.getId())
                .stream().map(TeacherClassSubject::getId).collect(Collectors.toSet());
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
        
        // Handle variations of frontend justified status
        if ("JUSTIFIED".equalsIgnoreCase(dto.status()) || "EXCUSED".equalsIgnoreCase(dto.status())) {
            attendance.setStatus(com.andretti101.escolaweb.model.enums.AttendanceStatus.JUSTIFIED_ABSENCE);
        } else {
            attendance.setStatus(com.andretti101.escolaweb.model.enums.AttendanceStatus.valueOf(dto.status().toUpperCase()));
        }
        
        return attendance;
    }

    private AttendanceResponseDTO toResponse(Attendance a) {
        return toResponse(a, null);
    }

    private AttendanceResponseDTO toResponse(Attendance a, List<com.andretti101.escolaweb.model.entity.AcademicPeriod> cachedPeriods) {
        String pName = "-";
        java.time.LocalDate d = a.getLesson().getLessonDate();
        if (d != null) {
            var periods = cachedPeriods != null ? cachedPeriods : academicPeriodService.findAll();
            for (var p : periods) {
                if (p.getStartDate() != null && p.getEndDate() != null) {
                    if (!d.isBefore(p.getStartDate()) && !d.isAfter(p.getEndDate())) {
                        pName = p.getName();
                        break;
                    }
                }
            }
        }

        return new AttendanceResponseDTO(
                a.getId(),
                a.getStudent().getId(),
                a.getStudent().getName(),
                a.getLesson().getId(),
                a.getLesson().getLessonDate(),
                a.getLesson().getTeacherClassSubject().getSubject().getName(),
                pName,
                a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1,
                a.getStatus(),
                a.getLesson().getContent(),
                a.getLesson().getNotes(),
                a.getCreatedAt()
        );
    }
}
