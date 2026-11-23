package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.response.PagedResponse;
import com.andretti101.escolaweb.dto.response.UserStatsDTO;
import com.andretti101.escolaweb.dto.response.UserSummaryDTO;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Controlador REST para consulta unificada de contas de usuários, métricas de acesso e papéis do sistema
@Tag(name = "Usuários", description = "Endpoints consolidados para consulta de usuários da escola com paginação e estatísticas")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Listar usuários consolidados", description = "Retorna lista consolidada de usuários com paginação e filtros opcionais por papel (role: STUDENT, TEACHER, SECRETARY, PRINCIPAL, STAFF) e status (active). Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<PagedResponse<UserSummaryDTO>> findAll(
            @Parameter(description = "Filtrar por papel (STUDENT, TEACHER, SECRETARY, PRINCIPAL, STAFF)")
            @RequestParam(required = false) java.util.List<String> role,
            @Parameter(description = "Filtrar por status ativo")
            @RequestParam(required = false) Boolean active,
            @Parameter(description = "Busca textual por nome, e-mail, matrícula ou turma")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filtrar situação acadêmica do aluno (APPROVED, FAILED, PENDING, UNENROLLED)")
            @RequestParam(required = false) String situation,
            @Parameter(description = "Filtrar por turma (nome ou ID)")
            @RequestParam(required = false) String className,
            @Parameter(description = "Filtrar por disciplina")
            @RequestParam(required = false) String subject,
            @ParameterObject Pageable pageable) {
        java.util.List<UserRole> resolvedRoles = parseRoles(role);
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasSituation = situation != null && !situation.isBlank() && !"ALL".equalsIgnoreCase(situation);
        boolean hasClassName = className != null && !className.isBlank() && !"ALL".equalsIgnoreCase(className);
        boolean hasSubject = subject != null && !subject.isBlank() && !"ALL".equalsIgnoreCase(subject);
        if (!hasSearch && !hasSituation && !hasClassName && !hasSubject) {
            return ResponseEntity.ok(userService.findSummaries(resolvedRoles, active, pageable));
        }
        return ResponseEntity.ok(userService.findSummaries(resolvedRoles, active, search, situation, className, subject, pageable));
    }

    private java.util.List<UserRole> parseRoles(java.util.List<String> roleParams) {
        if (roleParams == null || roleParams.isEmpty()) {
            return null;
        }
        java.util.List<UserRole> roles = new java.util.ArrayList<>();
        for (String param : roleParams) {
            if (param == null || param.isBlank()) continue;
            for (String item : param.split(",")) {
                String token = item.trim().toUpperCase();
                if (token.isEmpty()) continue;
                if ("STAFF".equals(token)) {
                    if (!roles.contains(UserRole.PRINCIPAL)) roles.add(UserRole.PRINCIPAL);
                    if (!roles.contains(UserRole.SECRETARY)) roles.add(UserRole.SECRETARY);
                } else {
                    roles.add(UserRole.valueOf(token));
                }
            }
        }
        return roles.isEmpty() ? null : roles;
    }

    @Operation(summary = "Estatísticas consolidadas de usuários", description = "Retorna métricas consolidadas dos usuários e alunos (total de usuários, gestores, professores, alunos, aprovados, pendentes e reprovados). Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estatísticas calculadas com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<UserStatsDTO> getStats(
            @Parameter(description = "Filtrar por status ativo (true, false ou vazio para todos)")
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(userService.getUserStats(active));
    }
}
