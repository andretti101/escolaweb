package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.response.AttendanceHistoryResponseDTO;
import com.andretti101.escolaweb.dto.response.GradeHistoryResponseDTO;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.AttendanceHistory;
import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.GradeHistory;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.GradeKind;
import com.andretti101.escolaweb.repository.AttendanceHistoryRepository;
import com.andretti101.escolaweb.repository.AttendanceRepository;
import com.andretti101.escolaweb.repository.GradeHistoryRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import com.andretti101.escolaweb.service.AuditHistoryService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditHistoryServiceImpl implements AuditHistoryService {

    static final int MAX_PAGE_SIZE = 100;
    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(9999, 1, 1, 0, 0);

    private final GradeHistoryRepository gradeHistoryRepository;
    private final AttendanceHistoryRepository attendanceHistoryRepository;
    private final GradeRepository gradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Override
    @Transactional(readOnly = true)
    public Page<GradeHistoryResponseDTO> searchGradeHistory(HistoryFilter filter, int page, int size) {
        HistoryFilter f = scoped(filter);
        return gradeHistoryRepository.search(
                f.teacherId(), f.classRoomId(), f.studentId(), f.yearId(), f.periodId(),
                fromBound(f.from()), toBound(f.to()), pageable(page, size)
        ).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AttendanceHistoryResponseDTO> searchAttendanceHistory(HistoryFilter filter, int page, int size) {
        HistoryFilter f = scoped(filter);
        return attendanceHistoryRepository.search(
                f.teacherId(), f.classRoomId(), f.studentId(), f.yearId(), f.periodId(),
                fromBound(f.from()), toBound(f.to()), pageable(page, size)
        ).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeHistoryResponseDTO> gradeTimeline(Integer gradeId) {
        Grade grade = gradeRepository.findWithTcsById(gradeId)
                .orElseThrow(() -> new EntityNotFoundException("Nota não encontrada com o ID: " + gradeId));
        authenticatedUserService.enforceTeacherOwnership(
                grade.getAssessment().getTeacherClassSubject());
        return gradeHistoryRepository.findTimelineByGradeId(gradeId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceHistoryResponseDTO> attendanceTimeline(Integer attendanceId) {
        Attendance attendance = attendanceRepository.findWithTcsById(attendanceId)
                .orElseThrow(() -> new EntityNotFoundException("Registro de frequência não encontrado com o ID: " + attendanceId));
        authenticatedUserService.enforceTeacherOwnership(
                attendance.getLesson().getTeacherClassSubject());
        return attendanceHistoryRepository.findTimelineByAttendanceId(attendanceId).stream().map(this::toDto).toList();
    }

    // ── Helpers

    /** Teachers are always restricted to their own TeacherClassSubjects, whatever teacherId was requested. */
    private HistoryFilter scoped(HistoryFilter filter) {
        HistoryFilter f = filter != null ? filter : new HistoryFilter(null, null, null, null, null, null, null);
        if (authenticatedUserService.isTeacher()) {
            Integer ownId = authenticatedUserService.getAuthenticatedTeacher().getId();
            return new HistoryFilter(ownId, f.classRoomId(), f.studentId(), f.yearId(), f.periodId(), f.from(), f.to());
        }
        return f;
    }

    private static Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("changedAt"), Sort.Order.desc("id")));
    }

    private static LocalDateTime fromBound(LocalDate from) {
        return from != null ? from.atStartOfDay() : MIN_DATE;
    }

    private static LocalDateTime toBound(LocalDate to) {
        if (to == null) return MAX_DATE;
        try {
            return to.plusDays(1).atStartOfDay();
        } catch (Exception e) {
            return MAX_DATE;
        }
    }

    private GradeHistoryResponseDTO toDto(GradeHistory h) {
        Grade g = h.getGrade();
        Assessment a = g.getAssessment();
        TeacherClassSubject tcs = a.getTeacherClassSubject();
        return new GradeHistoryResponseDTO(
                h.getId(),
                g.getId(),
                g.getStudent().getId(),
                g.getStudent().getName(),
                tcs.getClassRoom().getId(),
                tcs.getClassRoom().getName(),
                tcs.getSubject().getName(),
                tcs.getTeacher().getId(),
                tcs.getTeacher().getName(),
                a.getTitle(),
                a.getPeriod() != null ? a.getPeriod().getName() : null,
                h.getGradeKind() != null ? h.getGradeKind() : GradeKind.ORIGINAL,
                h.getPreviousGrade(),
                h.getNewGrade(),
                h.getNotes(),
                h.getChangedBy() != null ? h.getChangedBy().getName() : null,
                h.getChangedAt()
        );
    }

    private AttendanceHistoryResponseDTO toDto(AttendanceHistory h) {
        Attendance att = h.getAttendance();
        TeacherClassSubject tcs = att.getLesson().getTeacherClassSubject();
        return new AttendanceHistoryResponseDTO(
                h.getId(),
                att.getId(),
                att.getStudent().getId(),
                att.getStudent().getName(),
                tcs.getClassRoom().getId(),
                tcs.getClassRoom().getName(),
                tcs.getSubject().getName(),
                tcs.getTeacher().getId(),
                tcs.getTeacher().getName(),
                att.getLesson().getLessonDate(),
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getChangedBy() != null ? h.getChangedBy().getName() : null,
                h.getChangedAt()
        );
    }
}
