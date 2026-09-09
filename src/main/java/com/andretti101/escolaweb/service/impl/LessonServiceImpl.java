package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.LessonRepository;
import com.andretti101.escolaweb.service.LessonService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final AttendanceRepository attendanceRepository;
    private final com.andretti101.escolaweb.repository.AttendanceHistoryRepository attendanceHistoryRepository;
    private final TeacherClassSubjectService teacherClassSubjectService;

    @Override
    @Transactional
    public Lesson create(Lesson lesson) {
        if (lesson.getLessonDate() == null) {
            throw new IllegalArgumentException("Lesson date is required.");
        }

        TeacherClassSubject tcs = resolveTcs(lesson.getTeacherClassSubject());
        lesson.setTeacherClassSubject(tcs);

        return lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public Lesson update(Integer id, Lesson incoming) {
        Lesson existing = findLessonOrThrow(id);

        if (incoming.getLessonDate() == null) {
            throw new IllegalArgumentException("Lesson date is required.");
        }

        TeacherClassSubject tcs = resolveTcs(incoming.getTeacherClassSubject());

        existing.setLessonDate(incoming.getLessonDate());
        existing.setContent(incoming.getContent());
        existing.setNotes(incoming.getNotes());
        existing.setTeacherClassSubject(tcs);
        existing.setLessonCount(incoming.getLessonCount());

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Lesson lesson = findLessonOrThrow(id);

        List<com.andretti101.escolaweb.model.entity.Attendance> attendances = attendanceRepository.findByLesson(lesson);
        if (!attendances.isEmpty()) {
            for (com.andretti101.escolaweb.model.entity.Attendance attendance : attendances) {
                if (attendance.getHistory() != null && !attendance.getHistory().isEmpty()) {
                    attendanceHistoryRepository.deleteAll(attendance.getHistory());
                }
            }
            attendanceRepository.deleteAll(attendances);
        }

        lessonRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Lesson findById(Integer id) {
        return findLessonOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findAll() {
        return lessonRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findByTeacherClassSubject(Integer teacherClassSubjectId) {
        TeacherClassSubject tcs = teacherClassSubjectService.findById(teacherClassSubjectId);
        return lessonRepository.findByTeacherClassSubject(tcs);
    }

    Lesson findLessonOrThrow(Integer id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lesson not found with id: " + id));
    }

    private TeacherClassSubject resolveTcs(TeacherClassSubject ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("TeacherClassSubject is required.");
        }
        return teacherClassSubjectService.findById(ref.getId());
    }
}
