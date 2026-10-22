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
        // Vincula a aula ao professor, disciplina e turma no diário eletrônico
        if (lesson.getLessonDate() == null) {
            throw new IllegalArgumentException("A data da aula é obrigatória.");
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
            throw new IllegalArgumentException("A data da aula é obrigatória.");
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

        // Exclui em cascata frequências e históricos de presença vinculados à aula

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
    public org.springframework.data.domain.Page<Lesson> findAll(org.springframework.data.domain.Pageable pageable) {
        return lessonRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Lesson> findByTeacherActiveAcademicYear(Integer teacherId, org.springframework.data.domain.Pageable pageable) {
        return lessonRepository.findByTeacherActiveAcademicYear(teacherId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lesson> findByTeacherClassSubject(Integer teacherClassSubjectId) {
        TeacherClassSubject tcs = teacherClassSubjectService.findById(teacherClassSubjectId);
        return lessonRepository.findByTeacherClassSubject(tcs);
    }

    Lesson findLessonOrThrow(Integer id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Aula não encontrada."));
    }

    private TeacherClassSubject resolveTcs(TeacherClassSubject ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Selecione a disciplina e a turma correspondentes à aula.");
        }
        return teacherClassSubjectService.findById(ref.getId());
    }
}
