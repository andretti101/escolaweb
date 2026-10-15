package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.SecretaryRequestDTO;
import com.andretti101.escolaweb.dto.response.SecretaryResponseDTO;
import com.andretti101.escolaweb.model.entity.Secretary;
import com.andretti101.escolaweb.service.SecretaryService;
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

@Tag(name = "Secretários", description = "Endpoints para gerenciamento dos usuários com perfil de Secretaria Escolar")
@RestController
@RequestMapping("/api/secretaries")
@RequiredArgsConstructor
public class SecretaryController {

    private final SecretaryService secretaryService;

    @Operation(summary = "Cadastrar secretário(a)", description = "Cria um novo usuário com papel de Secretário escolar. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Secretário cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou e-mail já existente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SecretaryResponseDTO> create(@Valid @RequestBody SecretaryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(secretaryService.create(toEntity(dto))));
    }

    @Operation(summary = "Listar secretários", description = "Retorna uma lista paginada dos secretários cadastrados, com filtro opcional por status ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de secretários retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<org.springframework.data.domain.Page<SecretaryResponseDTO>> findAll(
            @Parameter(description = "Filtrar apenas secretários ativos") @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Secretary> page = activeOnly
                ? secretaryService.findAllActive(pageable)
                : secretaryService.findAll(pageable);
        List<SecretaryResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar secretário por ID", description = "Retorna os detalhes de um secretário específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secretário encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Secretário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SecretaryResponseDTO> findById(
            @Parameter(description = "ID do secretário") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(secretaryService.findById(id)));
    }

    @Operation(summary = "Atualizar secretário", description = "Atualiza os dados de cadastro de um secretário. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secretário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Secretário não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SecretaryResponseDTO> update(
            @Parameter(description = "ID do secretário") @PathVariable Integer id,
            @Valid @RequestBody SecretaryRequestDTO dto) {
        return ResponseEntity.ok(toResponse(secretaryService.update(id, toEntity(dto))));
    }

    @Operation(summary = "Excluir secretário", description = "Exclui o cadastro de um secretário do sistema. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Secretário excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Secretário não encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do secretário a ser excluído") @PathVariable Integer id) {
        secretaryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar secretário", description = "Reativa a conta de um secretário. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secretário ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Secretário não encontrado")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SecretaryResponseDTO> activate(
            @Parameter(description = "ID do secretário") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(secretaryService.activate(id)));
    }

    @Operation(summary = "Desativar secretário", description = "Desativa a conta de um secretário impedindo novos acessos. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secretário desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Secretário não encontrado")
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SecretaryResponseDTO> deactivate(
            @Parameter(description = "ID do secretário") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(secretaryService.deactivate(id)));
    }

    // ── Mapping

    private Secretary toEntity(SecretaryRequestDTO dto) {
        Secretary secretary = new Secretary();
        secretary.setName(dto.name());
        secretary.setEmail(dto.email());
        secretary.setPassword(dto.password());
        return secretary;
    }

    private SecretaryResponseDTO toResponse(Secretary s) {
        return new SecretaryResponseDTO(
                s.getId(),
                s.getName(),
                s.getEmail(),
                s.isActive(),
                s.getCreatedAt());
    }
}
