package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.response.GradeHistoryResponseDTO;
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
@RequestMapping("/api/grade-history")
@RequiredArgsConstructor
public class GradeHistoryController {

    private final AuditHistoryService auditHistoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<Page<GradeHistoryResponseDTO>> search(
            @RequestParam(required = false) Integer teacherId,
            @RequestParam(required = false) Integer classRoomId,
            @RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) Integer yearId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        HistoryFilter filter = new HistoryFilter(teacherId, classRoomId, studentId, yearId, from, to);
        return ResponseEntity.ok(auditHistoryService.searchGradeHistory(filter, page, size));
    }

    @GetMapping("/grade/{gradeId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<GradeHistoryResponseDTO>> timeline(@PathVariable Integer gradeId) {
        return ResponseEntity.ok(auditHistoryService.gradeTimeline(gradeId));
    }
}
