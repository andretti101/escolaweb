package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.response.AttendanceHistoryResponseDTO;
import com.andretti101.escolaweb.service.AuditHistoryService;
import com.andretti101.escolaweb.service.AuditHistoryService.HistoryFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance-history")
@RequiredArgsConstructor
public class AttendanceHistoryController {

    private final AuditHistoryService auditHistoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<Page<AttendanceHistoryResponseDTO>> search(
            @RequestParam(required = false) Integer teacherId,
            @RequestParam(required = false) Integer classRoomId,
            @RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) Integer yearId,
            @RequestParam(required = false) Integer periodId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        HistoryFilter filter = new HistoryFilter(teacherId, classRoomId, studentId, yearId, periodId, from, to);
        return ResponseEntity.ok(auditHistoryService.searchAttendanceHistory(filter, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @GetMapping("/attendance/{attendanceId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AttendanceHistoryResponseDTO>> timeline(@PathVariable Integer attendanceId) {
        return ResponseEntity.ok(auditHistoryService.attendanceTimeline(attendanceId));
    }
}
