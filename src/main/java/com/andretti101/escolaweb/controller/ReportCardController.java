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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Boletins Escolares", description = "Endpoints para consulta e emissão do boletim escolar consolidado de alunos")
@RestController
@RequestMapping("/api/report-cards")
@RequiredArgsConstructor
public class ReportCardController {

    private final ReportCardService reportCardService;
    private final AuthenticatedUserService authenticatedUserService;

    @Operation(summary = "Gerar boletim escolar do estudante", description = "Gera o boletim completo com notas de cada período, faltas, frequência percentual, média final e situação acadêmica (Aprovado, Reprovado, Em Recuperação).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Boletim gerado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (estudante só pode consultar seu próprio boletim)"),
            @ApiResponse(responseCode = "404", description = "Estudante ou ano letivo não encontrado")
    })
    @GetMapping("/student/{studentId}/year/{yearId}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ReportCardDTO> getReportCard(
            @Parameter(description = "ID do estudante") @PathVariable Integer studentId,
            @Parameter(description = "ID do ano letivo") @PathVariable Integer yearId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        ReportCardDTO reportCard = reportCardService.generateReportCard(studentId, yearId);
        return ResponseEntity.ok(reportCard);
    }
}
