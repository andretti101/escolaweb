package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.ReportCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report-cards")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/student/{studentId}/year/{yearId}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ReportCardDTO> getReportCard(
            @PathVariable Integer studentId,
            @PathVariable Integer yearId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        ReportCardDTO reportCard = reportCardService.generateReportCard(studentId, yearId);
        return ResponseEntity.ok(reportCard);
    }
}
