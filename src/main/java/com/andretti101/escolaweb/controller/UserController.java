package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.response.UserSummaryDTO;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Usuários", description = "Endpoints consolidados para consulta de usuários da escola")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Listar usuários consolidados", description = "Retorna lista consolidada de usuários com filtros opcionais por papel (role) e status (active). Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<List<UserSummaryDTO>> findAll(
            @Parameter(description = "Filtrar por papel (STUDENT, TEACHER, SECRETARY, PRINCIPAL)")
            @RequestParam(required = false) UserRole role,
            @Parameter(description = "Filtrar por status ativo")
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(userService.findSummaries(role, active));
    }
}
