package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.GradeLaunchRequestDTO;
import com.andretti101.escolaweb.dto.request.GradeUpdateRequestDTO;
import com.andretti101.escolaweb.dto.response.GradeResponseDTO;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.service.AssessmentService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.GradeService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notas e Avaliações", description = "Endpoints para lançamento, edição e cálculo de médias das notas escolares")
@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final AuthenticatedUserService authenticatedUserService;
    private final AssessmentService assessmentService;
    private final TeacherClassSubjectService teacherClassSubjectService;

    @Operation(summary = "Lançar nota do estudante", description = "Registra a nota obtida por um estudante em uma avaliação específica. Professores só lançam notas em suas próprias avaliações.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nota lançada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Valor de nota inválido ou estudante já avaliado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Estudante ou avaliação não encontrada")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<GradeResponseDTO> launch(@Valid @RequestBody GradeLaunchRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Assessment assessment = assessmentService.findById(dto.assessmentId());
            authenticatedUserService.enforceTeacherOwnership(assessment.getTeacherClassSubject());
        }
        Grade launched = gradeService.launch(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(launched));
    }

    @Operation(summary = "Listar todas as notas", description = "Retorna uma lista paginada de todas as notas registradas no sistema. Exclusivo para Secretaria e Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de notas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<PagedResponse<GradeResponseDTO>> findAll(
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        Page<Grade> page = gradeService.findAll(pageable);
        return ResponseEntity.ok(PagedResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getPageable(),
                page.getTotalElements()
        ));
    }

    @Operation(summary = "Buscar nota por ID", description = "Retorna o detalhamento de uma nota específica. Estudantes só acessam suas próprias notas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota encontrada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Nota não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<GradeResponseDTO> findById(
            @Parameter(description = "ID da nota lançada") @PathVariable Integer id) {
        Grade grade = gradeService.findById(id);
        authenticatedUserService.enforceStudentOwnership(grade.getStudent().getId());
        authenticatedUserService.enforceTeacherOwnership(grade.getAssessment().getTeacherClassSubject());
        return ResponseEntity.ok(toResponse(grade));
    }

    @Operation(summary = "Listar notas por estudante", description = "Retorna todas as notas de um estudante, com filtro opcional por ano letivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de notas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<List<GradeResponseDTO>> findByStudent(
            @Parameter(description = "ID do estudante") @PathVariable Integer studentId,
            @Parameter(description = "ID do ano letivo opcional") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer yearId) {
        authenticatedUserService.enforceStudentOwnership(studentId);

        List<Grade> grades = gradeService.findByStudent(studentId);

        if (yearId != null) {
            grades = grades.stream()
                    .filter(g -> g.getAssessment() != null && 
                                 g.getAssessment().getPeriod() != null && 
                                 g.getAssessment().getPeriod().getAcademicYear() != null &&
                                 g.getAssessment().getPeriod().getAcademicYear().getId().equals(yearId))
                    .toList();
        }

        if (authenticatedUserService.isTeacher()) {
            Set<Integer> myTcsIds = getAuthenticatedTeacherTcsIds();
            grades = grades.stream()
                    .filter(g -> myTcsIds.contains(g.getAssessment().getTeacherClassSubject().getId()))
                    .toList();
        }

        return ResponseEntity.ok(grades.stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Listar notas por avaliação", description = "Retorna todas as notas atribuídas aos estudantes para uma avaliação específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de notas da avaliação retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Avaliação não encontrada")
    })
    @GetMapping("/assessment/{assessmentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<GradeResponseDTO>> findByAssessment(
            @Parameter(description = "ID da avaliação") @PathVariable Integer assessmentId) {
        if (authenticatedUserService.isTeacher()) {
            Assessment assessment = assessmentService.findById(assessmentId);
            authenticatedUserService.enforceTeacherOwnership(assessment.getTeacherClassSubject());
        }
        return ResponseEntity.ok(
                gradeService.findByAssessment(assessmentId).stream().map(this::toResponse).toList()
        );
    }

    @Operation(summary = "Calcular média do estudante na disciplina", description = "Calcula a média ponderada/aritmética das notas do estudante para a atribuição docente especificada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Média calculada com sucesso (ex: 8.75)"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Estudante ou atribuição não encontrada")
    })
    @GetMapping("/average/student/{studentId}/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<BigDecimal> calculateAverage(
            @Parameter(description = "ID do estudante") @PathVariable Integer studentId,
            @Parameter(description = "ID da atribuição professor-turma-disciplina") @PathVariable Integer tcsId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(tcsId);
            authenticatedUserService.enforceTeacherOwnership(tcs);
        }
        return ResponseEntity.ok(gradeService.calculateAverage(studentId, tcsId));
    }

    @Operation(summary = "Atualizar nota", description = "Altera o valor numérico de uma nota lançada e grava o motivo da alteração no histórico de auditoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nota atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Novo valor inválido ou período encerrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Nota não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<GradeResponseDTO> update(
            @Parameter(description = "ID da nota") @PathVariable Integer id,
            @Valid @RequestBody GradeUpdateRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Grade existing = gradeService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(
                    existing.getAssessment().getTeacherClassSubject());
        }
        Grade incoming = new Grade();
        incoming.setValue(dto.value());
        return ResponseEntity.ok(toResponse(gradeService.update(id, incoming, dto.notes())));
    }

    @Operation(summary = "Excluir nota", description = "Remove um lançamento de nota do sistema. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Nota excluída com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Nota não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da nota a ser excluída") @PathVariable Integer id) {
        gradeService.delete(id);
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

    private Grade toEntity(GradeLaunchRequestDTO dto) {
        Student student = new Student();
        student.setId(dto.studentId());

        Assessment assessment = new Assessment();
        assessment.setId(dto.assessmentId());

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setAssessment(assessment);
        grade.setValue(dto.value());
        return grade;
    }

    private GradeResponseDTO toResponse(Grade g) {
        return new GradeResponseDTO(
                g.getId(),
                g.getStudent().getId(),
                g.getStudent().getName(),
                g.getAssessment().getId(),
                g.getAssessment().getTitle(),
                g.getAssessment().getAssessmentType().name(),
                g.getAssessment().getTeacherClassSubject().getId(),
                g.getAssessment().getTeacherClassSubject().getSubject().getName(),
                g.getAssessment().getPeriod().getName(),
                g.getValue(),
                g.getStudentSituation(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}
