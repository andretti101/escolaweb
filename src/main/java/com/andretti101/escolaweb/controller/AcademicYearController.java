package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AcademicYearRequestDTO;
import com.andretti101.escolaweb.dto.request.YearConclusionRequestDTO;
import com.andretti101.escolaweb.dto.response.AcademicYearResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.service.AcademicYearConclusionService;
import com.andretti101.escolaweb.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Anos Letivos", description = "Endpoints para gerenciamento do calendário e ciclo de vida dos anos letivos e processo de conclusão anual")
@RestController
@RequestMapping("/api/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;
    private final AcademicYearConclusionService conclusionService;

    @Operation(summary = "Criar ano letivo", description = "Cadastra um novo ano letivo definindo o ano civil, data de início e término. Apenas para o perfil Direção (PRINCIPAL).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ano letivo criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicYearResponseDTO> create(@Valid @RequestBody AcademicYearRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(academicYearService.create(toEntity(dto))));
    }

    @Operation(summary = "Listar anos letivos", description = "Retorna uma lista paginada de todos os anos letivos cadastrados no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de anos letivos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<org.springframework.data.domain.Page<AcademicYearResponseDTO>> findAll(
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<AcademicYear> page = academicYearService.findAll(pageable);
        List<AcademicYearResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Obter ano letivo ativo", description = "Retorna as informações do ano letivo atualmente marcado como ativo na instituição.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ano letivo ativo retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Nenhum ano letivo ativo no momento")
    })
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AcademicYearResponseDTO> findActive() {
        return ResponseEntity.ok(toResponse(academicYearService.findActive()));
    }

    @Operation(summary = "Buscar ano letivo por ID", description = "Retorna os detalhes de um ano letivo específico pelo seu identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ano letivo encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AcademicYearResponseDTO> findById(
            @Parameter(description = "ID do ano letivo") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicYearService.findById(id)));
    }

    @Operation(summary = "Atualizar ano letivo", description = "Altera as datas ou ano civil de um ano letivo existente. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ano letivo atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicYearResponseDTO> update(
            @Parameter(description = "ID do ano letivo") @PathVariable Integer id,
            @Valid @RequestBody AcademicYearRequestDTO dto) {
        return ResponseEntity.ok(toResponse(academicYearService.update(id, toEntity(dto))));
    }

    @Operation(summary = "Ativar ano letivo", description = "Define o ano letivo especificado como ativo e desativa automaticamente qualquer outro que estivesse ativo. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ano letivo ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicYearResponseDTO> setActive(
            @Parameter(description = "ID do ano letivo a ser ativado") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicYearService.setActive(id)));
    }

    @Operation(summary = "Excluir ano letivo", description = "Exclui um ano letivo caso não possua dependências impeditivas associadas. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ano letivo excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir o ano letivo ativo ou com dados vinculados"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do ano letivo a ser excluído") @PathVariable Integer id) {
        academicYearService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Verificar prontidão para conclusão do ano letivo", description = "Verifica se todos os períodos foram fechados e se todos os critérios acadêmicos estão satisfeitos para concluir o ano.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de aptidão retornado com sucesso")
    })
    @GetMapping("/conclusion-status")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<Map<String, Boolean>> getConclusionStatus() {
        return ResponseEntity.ok(Map.of("canConclude", conclusionService.canConcludeYear()));
    }

    @Operation(summary = "Concluir ano letivo ativo", description = "Executa o fechamento do ano letivo: calcula as notas finais de todos os alunos, apura aprovações/reprovações, gera o histórico consolidado e arquiva conteúdos. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ano letivo concluído com sucesso com resumo das operações"),
            @ApiResponse(responseCode = "400", description = "Não é possível concluir o ano (períodos em aberto ou inconsistências)")
    })
    @PostMapping("/conclude")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Map<String, Object>> concludeYear(
            @Valid @RequestBody YearConclusionRequestDTO request) {
        Map<String, Object> summary = conclusionService.concludeYear(request);
        return ResponseEntity.ok(summary);
    }

    //  Mapping

    private AcademicYear toEntity(AcademicYearRequestDTO dto) {
        AcademicYear year = new AcademicYear();
        year.setYear(dto.year());
        year.setStartDate(dto.startDate());
        year.setEndDate(dto.endDate());
        return year;
    }

    private AcademicYearResponseDTO toResponse(AcademicYear y) {
        return new AcademicYearResponseDTO(
                y.getId(),
                y.getYear(),
                y.getStartDate(),
                y.getEndDate(),
                y.isActive(),
                y.getCreatedAt());
    }
}
