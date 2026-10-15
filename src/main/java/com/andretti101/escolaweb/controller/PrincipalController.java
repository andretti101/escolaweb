package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.PrincipalRequestDTO;
import com.andretti101.escolaweb.dto.response.PrincipalResponseDTO;
import com.andretti101.escolaweb.model.entity.Principal;
import com.andretti101.escolaweb.service.PrincipalService;
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

@Tag(name = "Diretores", description = "Endpoints para gerenciamento dos usuários com perfil de Direção Escolar (Principal)")
@RestController
@RequestMapping("/api/principals")
@RequiredArgsConstructor
public class PrincipalController {

    private final PrincipalService principalService;

    @Operation(summary = "Cadastrar diretor(a)", description = "Cria um novo usuário com papel de Diretor (PRINCIPAL). Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Diretor cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou e-mail já existente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<PrincipalResponseDTO> create(@Valid @RequestBody PrincipalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(principalService.create(toEntity(dto))));
    }

    @Operation(summary = "Listar diretores", description = "Retorna uma lista paginada dos diretores cadastrados, com filtro opcional por status ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de diretores retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<org.springframework.data.domain.Page<PrincipalResponseDTO>> findAll(
            @Parameter(description = "Filtrar apenas diretores ativos") @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Principal> page = activeOnly
                ? principalService.findAllActive(pageable)
                : principalService.findAll(pageable);
        List<PrincipalResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar diretor por ID", description = "Retorna os detalhes de um diretor específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diretor encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Diretor não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<PrincipalResponseDTO> findById(
            @Parameter(description = "ID do diretor") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(principalService.findById(id)));
    }

    @Operation(summary = "Atualizar diretor", description = "Atualiza os dados de cadastro de um diretor. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diretor atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Diretor não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<PrincipalResponseDTO> update(
            @Parameter(description = "ID do diretor") @PathVariable Integer id,
            @Valid @RequestBody PrincipalRequestDTO dto) {
        return ResponseEntity.ok(toResponse(principalService.update(id, toEntity(dto))));
    }

    @Operation(summary = "Excluir diretor", description = "Exclui o cadastro de um diretor do sistema. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Diretor excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Diretor não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do diretor a ser excluído") @PathVariable Integer id) {
        principalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar diretor", description = "Reativa a conta de um diretor. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diretor ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Diretor não encontrado")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<PrincipalResponseDTO> activate(
            @Parameter(description = "ID do diretor") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(principalService.activate(id)));
    }

    @Operation(summary = "Desativar diretor", description = "Desativa a conta de um diretor impedindo novos acessos. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diretor desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Diretor não encontrado")
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<PrincipalResponseDTO> deactivate(
            @Parameter(description = "ID do diretor") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(principalService.deactivate(id)));
    }

    // ── Mapping

    private Principal toEntity(PrincipalRequestDTO dto) {
        Principal principal = new Principal();
        principal.setName(dto.name());
        principal.setEmail(dto.email());
        principal.setPassword(dto.password());
        return principal;
    }

    private PrincipalResponseDTO toResponse(Principal p) {
        return new PrincipalResponseDTO(
                p.getId(),
                p.getName(),
                p.getEmail(),
                p.isActive(),
                p.getCreatedAt());
    }
}
