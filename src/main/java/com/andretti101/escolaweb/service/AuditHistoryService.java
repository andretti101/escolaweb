package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.response.AttendanceHistoryResponseDTO;
import com.andretti101.escolaweb.dto.response.GradeHistoryResponseDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface AuditHistoryService {

    /** Filters for history searches. Any field may be null (= no filter). {@code to} is inclusive (whole day). */
    record HistoryFilter(Integer teacherId, Integer classRoomId, Integer studentId,
                         Integer yearId, LocalDate from, LocalDate to) {}

    Page<GradeHistoryResponseDTO> searchGradeHistory(HistoryFilter filter, int page, int size);

    Page<AttendanceHistoryResponseDTO> searchAttendanceHistory(HistoryFilter filter, int page, int size);

    /** Timeline of a single grade. Enforces teacher ownership when the caller is a teacher. */
    List<GradeHistoryResponseDTO> gradeTimeline(Integer gradeId);

    /** Timeline of a single attendance record. Enforces teacher ownership when the caller is a teacher. */
    List<AttendanceHistoryResponseDTO> attendanceTimeline(Integer attendanceId);
}
