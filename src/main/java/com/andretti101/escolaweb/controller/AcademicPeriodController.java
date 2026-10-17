package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AcademicPeriodRequestDTO;
import com.andretti101.escolaweb.dto.request.CreateDivisionRequestDTO;
import com.andretti101.escolaweb.dto.response.AcademicPeriodResponseDTO;
import com.andretti101.escolaweb.dto.response.DivisionResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Períodos Acadêmicos", description = "Endpoints para gerenciamento de períodos letivos (bimestres, trimestres, semestres) e divisões anuais")
@RestController
@RequestMapping("/api/academic-periods")
@RequiredArgsConstructor
public class AcademicPeriodController {

    private final AcademicPeriodService academicPeriodService;
    private final AcademicYearService academicYearService;
    private final ClassRoomRepository classRoomRepository;

    @Operation(summary = "Criar período acadêmico", description = "Cadastra individualmente um novo período acadêmico associado a um ano letivo. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Período criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicPeriodResponseDTO> create(@Valid @RequestBody AcademicPeriodRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(academicPeriodService.create(toEntity(dto))));
    }

    @Operation(summary = "Listar períodos acadêmicos", description = "Retorna uma lista paginada de todos os períodos acadêmicos cadastrados no sistema.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de períodos retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<org.springframework.data.domain.Page<AcademicPeriodResponseDTO>> findAll(
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<AcademicPeriod> page = academicPeriodService.findAll(pageable);
        List<AcademicPeriodResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar período por ID", description = "Retorna os detalhes de um período acadêmico específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Período encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Período acadêmico não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AcademicPeriodResponseDTO> findById(
            @Parameter(description = "ID do período acadêmico") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicPeriodService.findById(id)));
    }

    @Operation(summary = "Listar períodos por ano letivo", description = "Retorna todos os períodos acadêmicos de um determinado ano letivo, com filtro opcional por tipo (ex: BIMESTER, TRIMESTER).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de períodos retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @GetMapping("/academic-year/{yearId}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> findByAcademicYear(
            @Parameter(description = "ID do ano letivo") @PathVariable Integer yearId,
            @Parameter(description = "Tipo de período acadêmico opcional") @RequestParam(required = false) AcademicPeriodType type) {
        List<AcademicPeriod> list = (type != null)
                ? academicPeriodService.findByAcademicYearAndType(yearId, type)
                : academicPeriodService.findByAcademicYear(yearId);
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Obter divisões acadêmicas do ano", description = "Retorna o panorama das divisões acadêmicas existentes (bimestral, trimestral, etc.) em um ano letivo, incluindo status de turmas vinculadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Divisões retornadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @GetMapping("/divisions")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<DivisionResponseDTO>> getDivisions(
            @Parameter(description = "ID do ano letivo") @RequestParam Integer yearId) {
        AcademicYear year = academicYearService.findById(yearId);
        Map<AcademicPeriodType, List<AcademicPeriod>> divisions = academicPeriodService.findDivisions(yearId);
        List<DivisionResponseDTO> result = new ArrayList<>();

        for (Map.Entry<AcademicPeriodType, List<AcademicPeriod>> entry : divisions.entrySet()) {
            AcademicPeriodType type = entry.getKey();
            List<AcademicPeriodResponseDTO> periods = entry.getValue().stream()
                    .map(this::toResponse).toList();
            boolean hasActiveClasses = classRoomRepository.existsByAcademicYearAndPeriodTypeAndActiveTrue(year, type);
            result.add(new DivisionResponseDTO(type, type.getLabel(), type.getPeriodsCount(), hasActiveClasses, periods));
        }

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Criar divisão acadêmica automática", description = "Gera automaticamente o conjunto de períodos para um tipo de divisão (ex: 4 bimestres ou 3 trimestres) no ano letivo. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Divisão criada com sucesso com os períodos gerados"),
            @ApiResponse(responseCode = "400", description = "Divisão já existente ou parâmetros conflitantes")
    })
    @PostMapping("/divisions")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<List<AcademicPeriodResponseDTO>> createDivision(@Valid @RequestBody CreateDivisionRequestDTO dto) {
        List<AcademicPeriod> periods = academicPeriodService.createDivision(dto.academicYearId(), dto.periodType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(periods.stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Remover divisão acadêmica", description = "Exclui a divisão e todos os seus períodos associados caso não existam turmas vinculadas ativas. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Divisão removida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir divisão com turmas ativas")
    })
    @DeleteMapping("/divisions")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> deleteDivision(
            @Parameter(description = "ID do ano letivo") @RequestParam Integer yearId,
            @Parameter(description = "Tipo de período a remover") @RequestParam AcademicPeriodType type) {
        academicPeriodService.deleteDivision(yearId, type);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Atualizar período acadêmico", description = "Altera datas ou nome de um período acadêmico existente. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Período atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Período não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicPeriodResponseDTO> update(
            @Parameter(description = "ID do período acadêmico") @PathVariable Integer id,
            @Valid @RequestBody AcademicPeriodRequestDTO dto) {
        return ResponseEntity.ok(toResponse(academicPeriodService.update(id, toEntity(dto))));
    }

    @Operation(summary = "Fechar período acadêmico", description = "Encerra o período acadêmico atual, impedindo novos lançamentos e edições de notas/presenças sem autorização especial. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Período fechado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Período já se encontra fechado ou dados pendentes"),
            @ApiResponse(responseCode = "404", description = "Período não encontrado")
    })
    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicPeriodResponseDTO> close(
            @Parameter(description = "ID do período a ser fechado") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(academicPeriodService.close(id)));
    }

    @Operation(summary = "Excluir período acadêmico", description = "Exclui um período acadêmico isolado caso não possua registros vinculados. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Período excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir período com notas ou avaliações vinculadas"),
            @ApiResponse(responseCode = "404", description = "Período não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do período a ser excluído") @PathVariable Integer id) {
        academicPeriodService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Abrir próximo período acadêmico", description = "Abre o próximo período acadêmico sequencial da divisão no ano letivo. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Próximo período aberto com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não há próximo período a ser aberto")
    })
    @PutMapping("/open-next/{yearId}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<AcademicPeriodResponseDTO> openNextPeriod(
            @Parameter(description = "ID do ano letivo") @PathVariable Integer yearId,
            @Parameter(description = "Tipo de período opcional") @RequestParam(required = false) AcademicPeriodType type) {
        return ResponseEntity.ok(toResponse(academicPeriodService.openNextPeriod(yearId, type)));
    }

    // ── Mapping

    private AcademicPeriod toEntity(AcademicPeriodRequestDTO dto) {
        AcademicYear year = new AcademicYear();
        year.setId(dto.academicYearId());

        AcademicPeriod period = new AcademicPeriod();
        period.setName(dto.name());
        period.setPeriodType(dto.periodType());
        period.setStartDate(dto.startDate());
        period.setEndDate(dto.endDate());
        period.setAcademicYear(year);
        return period;
    }

    private AcademicPeriodResponseDTO toResponse(AcademicPeriod p) {
        Integer yearId = p.getAcademicYear() != null ? p.getAcademicYear().getId() : null;
        Integer yearNum = null;
        if (p.getAcademicYear() != null) {
            try {
                yearNum = p.getAcademicYear().getYear();
            } catch (Exception ignored) {
                if (yearId != null) {
                    try {
                        AcademicYear ay = academicYearService.findById(yearId);
                        if (ay != null) {
                            yearNum = ay.getYear();
                        }
                    } catch (Exception ignored2) {}
                }
            }
        }
        return new AcademicPeriodResponseDTO(
                p.getId(),
                p.getName(),
                p.getPeriodType(),
                p.getPeriodType() != null ? p.getPeriodType().getLabel() : null,
                p.getStartDate(),
                p.getEndDate(),
                p.isClosed(),
                yearId,
                yearNum,
                p.getCreatedAt());
    }
}
