package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.SubjectRequestDTO;
import com.andretti101.escolaweb.dto.response.SubjectResponseDTO;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Disciplinas", description = "Endpoints para gerenciamento do catálogo de disciplinas escolares")
@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @Operation(summary = "Criar disciplina", description = "Cadastra uma nova disciplina na matriz curricular informando nome e descrição. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Disciplina criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> create(@Valid @RequestBody SubjectRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(subjectService.create(toEntity(dto))));
    }

    @Operation(summary = "Listar disciplinas", description = "Retorna uma lista paginada de disciplinas cadastradas com opção de filtro por status ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de disciplinas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<org.springframework.data.domain.Page<SubjectResponseDTO>> findAll(
            @Parameter(description = "Filtrar apenas disciplinas ativas") @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Subject> page = activeOnly
                ? subjectService.findAllActive(pageable)
                : subjectService.findAll(pageable);
        List<SubjectResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar disciplina por ID", description = "Retorna os detalhes de uma disciplina específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disciplina encontrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<SubjectResponseDTO> findById(
            @Parameter(description = "ID da disciplina") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(subjectService.findById(id)));
    }

    @Operation(summary = "Atualizar disciplina", description = "Altera o nome ou descrição de uma disciplina existente. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disciplina atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> update(
            @Parameter(description = "ID da disciplina") @PathVariable Integer id,
            @Valid @RequestBody SubjectRequestDTO dto) {
        return ResponseEntity.ok(toResponse(subjectService.update(id, toEntity(dto))));
    }

    @Operation(summary = "Excluir disciplina", description = "Exclui uma disciplina caso não esteja associada a professores ou turmas. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Disciplina excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir disciplina vinculada a turmas"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da disciplina a ser excluída") @PathVariable Integer id) {
        subjectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar disciplina", description = "Reativa uma disciplina previamente desativada. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disciplina ativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> activate(
            @Parameter(description = "ID da disciplina") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(subjectService.activate(id)));
    }

    @Operation(summary = "Desativar disciplina", description = "Desativa uma disciplina, impedindo novas atribuições. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disciplina desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Disciplina não encontrada")
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<SubjectResponseDTO> deactivate(
            @Parameter(description = "ID da disciplina") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(subjectService.deactivate(id)));
    }

    // ── Mapping

    private Subject toEntity(SubjectRequestDTO dto) {
        Subject subject = new Subject();
        subject.setName(dto.name());
        subject.setDescription(dto.description());
        return subject;
    }

    private SubjectResponseDTO toResponse(Subject s) {
        return new SubjectResponseDTO(
                s.getId(),
                s.getName(),
                s.getDescription(),
                s.isActive(),
                s.getCreatedAt());
    }
}
