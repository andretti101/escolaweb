package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AssessmentRequestDTO;
import com.andretti101.escolaweb.dto.response.AssessmentResponseDTO;
import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.service.AssessmentService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.andretti101.escolaweb.dto.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

// Controlador REST para cadastro e consulta de instrumentos de avaliação (provas, trabalhos e recuperações)
@Tag(name = "Avaliações", description = "Endpoints para criação e gestão de instrumentos avaliativos (provas, trabalhos, testes)")
@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuthenticatedUserService authenticatedUserService;
    private final TeacherClassSubjectService teacherClassSubjectService;

    @Operation(summary = "Criar avaliação", description = "Cadastra uma nova avaliação informando título, tipo (EXAM, ASSIGNMENT, TEST, etc.), data de aplicação, período acadêmico e atribuição docente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Limite de avaliações do período excedido ou dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AssessmentResponseDTO> create(@Valid @RequestBody AssessmentRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(dto.teacherClassSubjectId());
            authenticatedUserService.enforceTeacherOwnership(tcs);
        }
        Assessment created = assessmentService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "Listar avaliações", description = "Retorna uma lista paginada de avaliações. Para professores, filtra automaticamente apenas as avaliações de suas disciplinas no ano letivo ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de avaliações retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<PagedResponse<AssessmentResponseDTO>> findAll(
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        if (authenticatedUserService.isTeacher()) {
            Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
            Page<Assessment> page = assessmentService.findByTeacherActiveAcademicYear(teacher.getId(), pageable);
            return ResponseEntity.ok(PagedResponse.of(
                    page.getContent().stream().map(this::toResponse).toList(),
                    page.getPageable(),
                    page.getTotalElements()
            ));
        }
        Page<Assessment> page = assessmentService.findAll(pageable);
        return ResponseEntity.ok(PagedResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getPageable(),
                page.getTotalElements()
        ));
    }

    @Operation(summary = "Buscar avaliação por ID", description = "Retorna os detalhes de uma avaliação específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação encontrada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<AssessmentResponseDTO> findById(
            @Parameter(description = "ID da avaliação") @PathVariable Integer id) {
        Assessment assessment = assessmentService.findById(id);
        authenticatedUserService.enforceTeacherOwnership(assessment.getTeacherClassSubject());
        authenticatedUserService.enforceStudentClassRoomOwnership(
                assessment.getTeacherClassSubject() != null ? assessment.getTeacherClassSubject().getClassRoom() : null);
        return ResponseEntity.ok(toResponse(assessment));
    }

    @Operation(summary = "Listar avaliações por atribuição", description = "Retorna todas as avaliações de uma atribuição professor-turma-disciplina.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de avaliações retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Atribuição não encontrada")
    })
    @GetMapping("/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AssessmentResponseDTO>> findByTeacherClassSubject(
            @Parameter(description = "ID da atribuição professor-turma-disciplina") @PathVariable Integer tcsId) {
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(tcsId);
            authenticatedUserService.enforceTeacherOwnership(tcs);
        }
        return ResponseEntity.ok(
                assessmentService.findByTeacherClassSubject(tcsId).stream().map(this::toResponse).toList()
        );
    }

    @Operation(summary = "Listar avaliações por período acadêmico", description = "Retorna as avaliações cadastradas em um período acadêmico específico (ex: 1º Bimestre).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de avaliações do período retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Período acadêmico não encontrado")
    })
    @GetMapping("/period/{periodId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AssessmentResponseDTO>> findByPeriod(
            @Parameter(description = "ID do período acadêmico") @PathVariable Integer periodId) {
        List<Assessment> assessments = assessmentService.findByPeriod(periodId);

        if (authenticatedUserService.isTeacher()) {
            Set<Integer> myTcsIds = getAuthenticatedTeacherTcsIds();
            assessments = assessments.stream()
                    .filter(a -> myTcsIds.contains(a.getTeacherClassSubject().getId()))
                    .toList();
        }

        return ResponseEntity.ok(assessments.stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Atualizar avaliação", description = "Atualiza o título, tipo, data ou atribuição de uma avaliação existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AssessmentResponseDTO> update(
            @Parameter(description = "ID da avaliação") @PathVariable Integer id,
            @Valid @RequestBody AssessmentRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Assessment existing = assessmentService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(existing.getTeacherClassSubject());
            TeacherClassSubject newTcs = teacherClassSubjectService.findById(dto.teacherClassSubjectId());
            authenticatedUserService.enforceTeacherOwnership(newTcs);
        }
        Assessment updated = assessmentService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Excluir avaliação", description = "Remove uma avaliação caso não possua notas lançadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Avaliação excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir avaliação com notas lançadas"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da avaliação a ser excluída") @PathVariable Integer id) {
        if (authenticatedUserService.isTeacher()) {
            Assessment existing = assessmentService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(existing.getTeacherClassSubject());
        }
        assessmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers

    private Set<Integer> getAuthenticatedTeacherTcsIds() {
        Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
        return teacherClassSubjectService.findByTeacher(teacher.getId())
                .stream()
                .filter(tcs -> tcs.getClassRoom().getAcademicYear().isActive())
                .map(TeacherClassSubject::getId)
                .collect(Collectors.toSet());
    }

    // ── Mapping

    private Assessment toEntity(AssessmentRequestDTO dto) {
        TeacherClassSubject tcs = new TeacherClassSubject();
        tcs.setId(dto.teacherClassSubjectId());

        AcademicPeriod period = new AcademicPeriod();
        period.setId(dto.periodId());

        Assessment assessment = new Assessment();
        assessment.setTitle(dto.title());
        assessment.setDescription(dto.description());
        assessment.setDate(dto.date());
        assessment.setAssessmentType(dto.assessmentType());
        assessment.setTeacherClassSubject(tcs);
        assessment.setPeriod(period);
        return assessment;
    }

    private AssessmentResponseDTO toResponse(Assessment a) {
        return new AssessmentResponseDTO(
                a.getId(),
                a.getTitle(),
                a.getDescription(),
                a.getDate(),
                a.getAssessmentType(),
                a.getTeacherClassSubject().getId(),
                a.getTeacherClassSubject().getSubject().getName(),
                a.getTeacherClassSubject().getTeacher().getName(),
                a.getTeacherClassSubject().getClassRoom().getId(),
                a.getTeacherClassSubject().getClassRoom().getName(),
                a.getPeriod().getId(),
                a.getPeriod().getName(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
