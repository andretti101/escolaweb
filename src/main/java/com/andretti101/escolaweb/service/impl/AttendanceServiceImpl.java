package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.AttendanceHistory;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.AttendanceStatus;
import com.andretti101.escolaweb.repository.AttendanceHistoryRepository;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.LessonRepository;
import com.andretti101.escolaweb.service.AttendanceService;
import com.andretti101.escolaweb.service.LessonService;
import com.andretti101.escolaweb.service.StudentService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceHistoryRepository attendanceHistoryRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonService lessonService;
    private final StudentService studentService;
    private final TeacherClassSubjectService teacherClassSubjectService;

    @Override
    @Transactional
    public Attendance register(Attendance attendance) {
        Lesson lesson = resolveLesson(attendance.getLesson());
        Student student = resolveStudent(attendance.getStudent());

        if (attendance.getStatus() == null) {
            throw new IllegalArgumentException("Attendance status is required.");
        }
        if (attendanceRepository.existsByLessonAndStudent(lesson, student)) {
            throw new IllegalStateException(
                    "Attendance for student with id " + student.getId()
                            + " in lesson with id " + lesson.getId() + " is already registered.");
        }

        validateActiveEnrollment(student, lesson.getTeacherClassSubject());

        attendance.setLesson(lesson);
        attendance.setStudent(student);

        Attendance saved = attendanceRepository.save(attendance);

        recordHistory(saved, null, saved.getStatus());

        return saved;
    }

    @Override
    @Transactional
    public Attendance update(Integer id, Attendance incoming) {
        Attendance existing = findAttendanceOrThrow(id);

        if (incoming.getStatus() == null) {
            throw new IllegalArgumentException("Attendance status is required.");
        }

        AttendanceStatus previous = existing.getStatus();

        existing.setStatus(incoming.getStatus());
        existing.setNotes(incoming.getNotes());

        if (previous != incoming.getStatus()) {
            recordHistory(existing, previous, existing.getStatus());
        }

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        findAttendanceOrThrow(id);
        attendanceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Attendance findById(Integer id) {
        return findAttendanceOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findAll() {
        return attendanceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findByLesson(Integer lessonId) {
        Lesson lesson = lessonService.findById(lessonId);
        return attendanceRepository.findByLesson(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> findByStudent(Integer studentId) {
        Student student = studentService.findById(studentId);
        return attendanceRepository.findByStudent(student);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateFrequency(Integer studentId, Integer teacherClassSubjectId) {
        Student student = studentService.findById(studentId);
        TeacherClassSubject tcs = teacherClassSubjectService.findById(teacherClassSubjectId);

        List<Lesson> lessons = lessonRepository.findByTeacherClassSubject(tcs);

        if (lessons.isEmpty()) {
            return BigDecimal.valueOf(100);
        }
        int totalSlots = lessons.stream()
                .mapToInt(lesson -> lesson.getLessonCount().getValue())
                .sum();

        Map<Integer, Integer> slotsByLessonId = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l.getLessonCount().getValue()));

        int absentSlots = attendanceRepository
                .findByLessonInAndStudentAndStatus(lessons, student, AttendanceStatus.ABSENT)
                .stream()
                .mapToInt(a -> slotsByLessonId.getOrDefault(a.getLesson().getId(), 1))
                .sum();

        return BigDecimal.valueOf(totalSlots - absentSlots)
                .divide(BigDecimal.valueOf(totalSlots), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void recordHistory(Attendance attendance, AttendanceStatus previous, AttendanceStatus current) {
        AttendanceHistory history = AttendanceHistory.builder()
                .attendance(attendance)
                .previousStatus(previous != null ? previous.name() : null)
                .newStatus(current != null ? current.name() : null)
                .build();
        attendanceHistoryRepository.save(history);
    }

    Attendance findAttendanceOrThrow(Integer id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Attendance not found with id: " + id));
    }

    private void validateActiveEnrollment(Student student, TeacherClassSubject tcs) {
        if (!enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, tcs.getClassRoom())) {
            throw new IllegalStateException(
                    "Student with id " + student.getId()
                    + " does not have an active enrollment in classroom with id "
                    + tcs.getClassRoom().getId() + ".");
        }
    }

    private Lesson resolveLesson(Lesson ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Lesson is required.");
        }
        return lessonService.findById(ref.getId());
    }

    private Student resolveStudent(Student ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Student is required.");
        }
        return studentService.findById(ref.getId());
    }
}