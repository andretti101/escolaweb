package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.AttendanceHistory;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AttendanceHistoryRepository;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.LessonRepository;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.LessonService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceHistoryRepository attendanceHistoryRepository;
    private final TeacherClassSubjectService teacherClassSubjectService;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional
    public Lesson create(Lesson lesson) {
        // Vincula a aula ao professor, disciplina e turma no diário eletrônico
        if (lesson.getLessonDate() == null) {
            throw new IllegalArgumentException("A data da aula é obrigatória.");
        }
        validateLessonDateNotFuture(lesson.getLessonDate());

        TeacherClassSubject tcs = resolveTcs(lesson.getTeacherClassSubject());
        validateAcademicYearActive(tcs);
        validateLessonPeriodOpen(lesson.getLessonDate(), tcs);
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
        validateLessonDateNotFuture(incoming.getLessonDate());

        validateAcademicYearActive(existing.getTeacherClassSubject());
        validateLessonPeriodOpen(existing.getLessonDate(), existing.getTeacherClassSubject());

        TeacherClassSubject tcs = resolveTcs(incoming.getTeacherClassSubject());
        validateAcademicYearActive(tcs);
        validateLessonPeriodOpen(incoming.getLessonDate(), tcs);

        boolean lessonCountChanged = existing.getLessonCount() != incoming.getLessonCount();
        Integer oldTcsId = existing.getTeacherClassSubject() != null ? existing.getTeacherClassSubject().getId() : null;
        Integer newTcsId = tcs.getId();
        boolean tcsChanged = !Objects.equals(oldTcsId, newTcsId);

        existing.setLessonDate(incoming.getLessonDate());
        existing.setContent(incoming.getContent());
        existing.setNotes(incoming.getNotes());
        existing.setTeacherClassSubject(tcs);
        existing.setLessonCount(incoming.getLessonCount());

        if ((lessonCountChanged || tcsChanged) && applicationEventPublisher != null) {
            List<Attendance> attendances = attendanceRepository.findByLesson(existing);
            if (attendances != null && !attendances.isEmpty()) {
                List<Integer> affectedStudentIds = attendances.stream()
                        .map(Attendance::getStudent)
                        .filter(Objects::nonNull)
                        .map(Student::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                for (Integer studentId : affectedStudentIds) {
                    if (newTcsId != null) {
                        applicationEventPublisher.publishEvent(
                                new AttendanceServiceImpl.AttendanceChangedEvent(studentId, newTcsId));
                    }
                    if (tcsChanged && oldTcsId != null) {
                        applicationEventPublisher.publishEvent(
                                new AttendanceServiceImpl.AttendanceChangedEvent(studentId, oldTcsId));
                    }
                }
            }
        }

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Lesson lesson = findLessonOrThrow(id);
        validateAcademicYearActive(lesson.getTeacherClassSubject());
        if (lesson.getTeacherClassSubject() != null) {
            validateLessonPeriodOpen(lesson.getLessonDate(), lesson.getTeacherClassSubject());
        }

        List<Attendance> attendances = attendanceRepository.findByLesson(lesson);
        Integer tcsId = lesson.getTeacherClassSubject() != null ? lesson.getTeacherClassSubject().getId() : null;
        List<Integer> affectedStudentIds = attendances != null
                ? attendances.stream()
                        .map(Attendance::getStudent)
                        .filter(Objects::nonNull)
                        .map(Student::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList()
                : List.of();

        if (attendances != null && !attendances.isEmpty()) {
            for (Attendance attendance : attendances) {
                if (attendance.getHistory() != null && !attendance.getHistory().isEmpty()) {
                    attendanceHistoryRepository.deleteAll(attendance.getHistory());
                }
            }
            attendanceRepository.deleteAll(attendances);
        }

        lessonRepository.deleteById(id);
        lessonRepository.flush();

        if (applicationEventPublisher != null && tcsId != null && !affectedStudentIds.isEmpty()) {
            for (Integer studentId : affectedStudentIds) {
                applicationEventPublisher.publishEvent(
                        new AttendanceServiceImpl.AttendanceChangedEvent(studentId, tcsId));
            }
        }
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

    private void validateAcademicYearActive(TeacherClassSubject tcs) {
        if (tcs != null && tcs.getClassRoom() != null && tcs.getClassRoom().getAcademicYear() != null
                && !tcs.getClassRoom().getAcademicYear().isActive()) {
            throw new IllegalStateException(
                    "O ano letivo '" + tcs.getClassRoom().getAcademicYear().getYear()
                    + "' não está ativo. Não é permitido criar, alterar ou excluir aulas de anos letivos encerrados.");
        }
    }

    private void validateLessonDateNotFuture(LocalDate lessonDate) {
        if (lessonDate != null && lessonDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Não é permitido registrar ou alterar aulas em datas futuras.");
        }
    }

    private void validateLessonPeriodOpen(LocalDate lessonDate, TeacherClassSubject tcs) {
        if (academicPeriodRepository == null || lessonDate == null || tcs == null
                || tcs.getClassRoom() == null
                || tcs.getClassRoom().getAcademicYear() == null
                || tcs.getClassRoom().getPeriodType() == null) {
            return;
        }
        List<AcademicPeriod> periods = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(
                tcs.getClassRoom().getAcademicYear(),
                tcs.getClassRoom().getPeriodType());
        if (periods == null || periods.isEmpty()) {
            return;
        }
        List<AcademicPeriod> matching = periods.stream()
                .filter(p -> p.getStartDate() != null && p.getEndDate() != null
                        && !lessonDate.isBefore(p.getStartDate())
                        && !lessonDate.isAfter(p.getEndDate()))
                .toList();
        if (matching.isEmpty()) {
            boolean hasConfiguredDates = periods.stream()
                    .anyMatch(p -> p.getStartDate() != null && p.getEndDate() != null);
            if (hasConfiguredDates) {
                throw new IllegalArgumentException(
                        "A data da aula não pertence a nenhum período letivo configurado para esta turma.");
            }
            if (periods.stream().allMatch(AcademicPeriod::isClosed)) {
                throw new IllegalStateException(
                        "Todos os períodos letivos desta turma estão fechados. Não é permitido criar, alterar ou excluir aulas.");
            }
            return;
        }
        if (matching.stream().noneMatch(p -> !p.isClosed())) {
            throw new IllegalStateException(
                    "O período letivo '" + matching.get(0).getName()
                    + "' está fechado. Não é permitido criar, alterar ou excluir aulas neste período.");
        }
    }
}
