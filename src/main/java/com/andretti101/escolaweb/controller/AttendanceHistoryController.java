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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;

// Controlador REST para consulta de trilha de auditoria e linha do tempo de retificações de presenças
@Tag(name = "Histórico de Frequência", description = "Endpoints de auditoria e linha do tempo de alterações nos registros de frequência escolar")
@RestController
@RequestMapping("/api/attendance-history")
@RequiredArgsConstructor
public class AttendanceHistoryController {

    private final AuditHistoryService auditHistoryService;

    @Operation(summary = "Pesquisar histórico de alterações de frequência", description = "Consulta paginada dos registros de auditoria de alterações de presença/falta com filtros combinados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de registros de auditoria retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<Page<AttendanceHistoryResponseDTO>> search(
            @Parameter(description = "ID do professor que realizou a alteração") @RequestParam(required = false) Integer teacherId,
            @Parameter(description = "ID da turma") @RequestParam(required = false) Integer classRoomId,
            @Parameter(description = "ID do estudante") @RequestParam(required = false) Integer studentId,
            @Parameter(description = "ID do ano letivo") @RequestParam(required = false) Integer yearId,
            @Parameter(description = "ID do período acadêmico") @RequestParam(required = false) Integer periodId,
            @Parameter(description = "Data inicial (AAAA-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final (AAAA-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @org.springdoc.core.annotations.ParameterObject @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        HistoryFilter filter = new HistoryFilter(teacherId, classRoomId, studentId, yearId, periodId, from, to);
        return ResponseEntity.ok(auditHistoryService.searchAttendanceHistory(filter, pageable.getPageNumber(), pageable.getPageSize()));
    }

    @Operation(summary = "Linha do tempo de uma presença", description = "Retorna o histórico cronológico completo de todas as alterações sofridas por um registro de frequência específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linha do tempo retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Registro de frequência não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/attendance/{attendanceId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AttendanceHistoryResponseDTO>> timeline(
            @Parameter(description = "ID do registro de frequência") @PathVariable Integer attendanceId) {
        return ResponseEntity.ok(auditHistoryService.attendanceTimeline(attendanceId));
    }
}
