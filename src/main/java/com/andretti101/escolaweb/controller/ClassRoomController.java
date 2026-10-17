package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ClassRoomRequestDTO;
import com.andretti101.escolaweb.dto.response.AcademicPeriodResponseDTO;
import com.andretti101.escolaweb.dto.response.ClassRoomResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.service.ClassRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Turmas", description = "Endpoints para gerenciamento de turmas escolares, turnos, séries e vinculação ao ano letivo")
@RestController
@RequestMapping("/api/classrooms")
@RequiredArgsConstructor
public class ClassRoomController {

    private final ClassRoomService classRoomService;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final com.andretti101.escolaweb.repository.AcademicYearRepository academicYearRepository;

    @Operation(summary = "Criar turma", description = "Cadastra uma nova turma informando nome, turno, série escolar, tipo de divisão de períodos e ano letivo. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Turma criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> create(@Valid @RequestBody ClassRoomRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(classRoomService.create(toEntity(dto))));
    }

    @Operation(summary = "Listar turmas", description = "Retorna uma lista paginada de turmas com suporte a filtro por status ativo/inativo e ano letivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de turmas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<org.springframework.data.domain.Page<ClassRoomResponseDTO>> findAll(
            @Parameter(description = "Filtrar apenas turmas ativas (padrão: true)") @RequestParam(required = false, defaultValue = "true") boolean activeOnly,
            @Parameter(description = "ID do ano letivo para filtro") @RequestParam(required = false) Integer academicYearId,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<ClassRoom> page;
        if (academicYearId != null) {
            page = activeOnly
                    ? classRoomService.findByAcademicYearAndActiveTrue(academicYearId, pageable)
                    : classRoomService.findByAcademicYear(academicYearId, pageable);
        } else {
            page = activeOnly
                    ? classRoomService.findAllActive(pageable)
                    : classRoomService.findAll(pageable);
        }
        List<ClassRoomResponseDTO> dtoList = toResponseList(page.getContent());
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar turma por ID", description = "Retorna as informações completas de uma turma específica, incluindo períodos acadêmicos associados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turma encontrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ClassRoomResponseDTO> findById(
            @Parameter(description = "ID da turma") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(classRoomService.findById(id)));
    }

    @Operation(summary = "Listar turmas por ano letivo", description = "Retorna todas as turmas cadastradas em um determinado ano letivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de turmas retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @GetMapping("/academic-year/{yearId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ClassRoomResponseDTO>> findByAcademicYear(
            @Parameter(description = "ID do ano letivo") @PathVariable Integer yearId) {
        return ResponseEntity.ok(toResponseList(classRoomService.findByAcademicYear(yearId)));
    }

    @Operation(summary = "Atualizar turma", description = "Atualiza os dados de identificação, turno, série ou divisão de uma turma. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turma atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> update(
            @Parameter(description = "ID da turma") @PathVariable Integer id,
            @Valid @RequestBody ClassRoomRequestDTO dto) {
        return ResponseEntity.ok(toResponse(classRoomService.update(id, toEntity(dto))));
    }

    @Operation(summary = "Excluir turma", description = "Exclui uma turma caso não possua alunos matriculados ou aulas vinculadas. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Turma excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir turma com matrículas ativas"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da turma a ser excluída") @PathVariable Integer id) {
        classRoomService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar turma", description = "Reativa uma turma desativada. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turma ativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> activate(
            @Parameter(description = "ID da turma") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(classRoomService.activate(id)));
    }

    @Operation(summary = "Desativar turma", description = "Desativa uma turma, impedindo novas matrículas. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Turma desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<ClassRoomResponseDTO> deactivate(
            @Parameter(description = "ID da turma") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(classRoomService.deactivate(id)));
    }

    // ── Mapping

    private ClassRoom toEntity(ClassRoomRequestDTO dto) {
        AcademicYear year = new AcademicYear();
        year.setId(dto.academicYearId());

        ClassRoom classRoom = new ClassRoom();
        classRoom.setName(dto.name());
        classRoom.setShift(dto.shift());
        classRoom.setPeriodType(dto.periodType());
        classRoom.setSchoolGrade(dto.schoolGrade());
        classRoom.setAcademicYear(year);
        return classRoom;
    }

    private List<ClassRoomResponseDTO> toResponseList(List<ClassRoom> classRooms) {
        if (classRooms == null || classRooms.isEmpty()) {
            return List.of();
        }
        Set<Integer> yearIds = classRooms.stream()
                .map(ClassRoom::getAcademicYear)
                .filter(Objects::nonNull)
                .map(AcademicYear::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, AcademicYear> yearsById = yearIds.isEmpty()
                ? Map.of()
                : academicYearRepository.findAllById(yearIds).stream()
                        .collect(Collectors.toMap(AcademicYear::getId, y -> y, (a, b) -> a));

        Map<String, List<AcademicPeriodResponseDTO>> periodsByYearAndType = yearIds.isEmpty()
                ? Map.of()
                : academicPeriodRepository.findByAcademicYearIdsInOrderByIdAsc(yearIds).stream()
                        .map(p -> toPeriodResponse(p, yearsById))
                        .collect(Collectors.groupingBy(p -> p.academicYearId() + "_" + (p.periodType() != null ? p.periodType().name() : "")));

        return classRooms.stream().map(c -> {
            Integer yearId = c.getAcademicYear() != null ? c.getAcademicYear().getId() : null;
            String key = yearId + "_" + (c.getPeriodType() != null ? c.getPeriodType().name() : "");
            List<AcademicPeriodResponseDTO> periods = periodsByYearAndType.getOrDefault(key, List.of());
            return toResponseWithPeriods(c, periods, yearsById);
        }).toList();
    }

    private ClassRoomResponseDTO toResponse(ClassRoom c) {
        if (c == null) {
            return null;
        }
        return toResponseList(List.of(c)).get(0);
    }

    private ClassRoomResponseDTO toResponseWithPeriods(ClassRoom c, List<AcademicPeriodResponseDTO> periods, Map<Integer, AcademicYear> yearsById) {
        Integer yearId = c.getAcademicYear() != null ? c.getAcademicYear().getId() : null;
        Integer yearNum = null;
        if (yearId != null && yearsById != null && yearsById.containsKey(yearId)) {
            yearNum = yearsById.get(yearId).getYear();
        } else if (c.getAcademicYear() != null) {
            try {
                yearNum = c.getAcademicYear().getYear();
            } catch (Exception ignored) {}
        }
        if (yearNum == null && periods != null && !periods.isEmpty()) {
            yearNum = periods.get(0).academicYear();
        }

        return new ClassRoomResponseDTO(
                c.getId(),
                c.getName(),
                c.getShift(),
                c.getSchoolGrade(),
                c.getSchoolGrade() != null ? c.getSchoolGrade().getLabel() : null,
                c.getPeriodType(),
                c.getPeriodType() != null ? c.getPeriodType().getLabel() : null,
                yearId,
                yearNum,
                c.isActive(),
                c.getCreatedAt(),
                periods != null ? periods : List.of());
    }

    private AcademicPeriodResponseDTO toPeriodResponse(AcademicPeriod p, Map<Integer, AcademicYear> yearsById) {
        Integer yearId = p.getAcademicYear() != null ? p.getAcademicYear().getId() : null;
        Integer yearNum = null;
        if (yearId != null && yearsById != null && yearsById.containsKey(yearId)) {
            yearNum = yearsById.get(yearId).getYear();
        } else if (p.getAcademicYear() != null) {
            try {
                yearNum = p.getAcademicYear().getYear();
            } catch (Exception ignored) {}
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
