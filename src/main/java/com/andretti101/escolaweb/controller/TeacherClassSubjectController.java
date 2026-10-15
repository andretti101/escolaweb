package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.TeacherClassSubjectRequestDTO;
import com.andretti101.escolaweb.dto.response.TeacherClassSubjectResponseDTO;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
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

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Atribuições Professor-Turma-Disciplina", description = "Endpoints para vínculo pedagógico entre professor, turma e disciplina, incluindo limites de avaliações")
@RestController
@RequestMapping("/api/teacher-class-subjects")
@RequiredArgsConstructor
public class TeacherClassSubjectController {

    private final TeacherClassSubjectService teacherClassSubjectService;
    private final AuthenticatedUserService authenticatedUserService;

    @Operation(summary = "Criar atribuição professor-turma-disciplina", description = "Vincula um professor a uma turma e disciplina com parametrização de mínimo e máximo de avaliações por período. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Atribuição criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou duplicados"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherClassSubjectResponseDTO> create(
            @Valid @RequestBody TeacherClassSubjectRequestDTO dto) {
        TeacherClassSubject created = teacherClassSubjectService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "Listar atribuições professor-turma-disciplina", description = "Retorna uma lista paginada de atribuições. Para professores, filtra automaticamente apenas suas atribuições do ano letivo ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de atribuições retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER')")
    public ResponseEntity<org.springframework.data.domain.Page<TeacherClassSubjectResponseDTO>> findAll(
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<TeacherClassSubject> page;
        if (authenticatedUserService.isTeacher()) {
            Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
            page = teacherClassSubjectService.findByTeacherActiveAcademicYear(teacher.getId(), pageable);
        } else {
            page = teacherClassSubjectService.findAll(pageable);
        }
        List<TeacherClassSubjectResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar atribuição por ID", description = "Retorna os detalhes de uma atribuição específica. Professores só acessam suas próprias atribuições.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atribuição encontrada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado a atribuição de outro professor"),
            @ApiResponse(responseCode = "404", description = "Atribuição não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<TeacherClassSubjectResponseDTO> findById(
            @Parameter(description = "ID da atribuição") @PathVariable Integer id) {
        TeacherClassSubject tcs = teacherClassSubjectService.findById(id);
        authenticatedUserService.enforceTeacherOwnership(tcs);
        return ResponseEntity.ok(toResponse(tcs));
    }

    @Operation(summary = "Listar atribuições por professor", description = "Retorna todas as atribuições pedagógicas vinculadas a um determinado professor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de atribuições retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER')")
    public ResponseEntity<List<TeacherClassSubjectResponseDTO>> findByTeacher(
            @Parameter(description = "ID do professor") @PathVariable Integer teacherId) {
        if (authenticatedUserService.isTeacher()) {
            return ResponseEntity.ok(
                    teacherClassSubjectService.findByTeacher(teacherId).stream()
                            .filter(tcs -> tcs.getClassRoom().getAcademicYear().isActive())
                            .map(this::toResponse).toList()
            );
        }
        return ResponseEntity.ok(
                teacherClassSubjectService.findByTeacher(teacherId).stream().map(this::toResponse).toList()
        );
    }

    @Operation(summary = "Listar atribuições por turma", description = "Retorna a grade curricular com os professores e disciplinas alocados a uma turma específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de atribuições da turma retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @GetMapping("/classroom/{classRoomId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<TeacherClassSubjectResponseDTO>> findByClassRoom(
            @Parameter(description = "ID da turma") @PathVariable Integer classRoomId) {
        if (authenticatedUserService.isTeacher()) {
            return ResponseEntity.ok(
                    teacherClassSubjectService.findByClassRoom(classRoomId).stream()
                            .filter(tcs -> tcs.getClassRoom().getAcademicYear().isActive())
                            .map(this::toResponse).toList()
            );
        }
        return ResponseEntity.ok(
                teacherClassSubjectService.findByClassRoom(classRoomId).stream().map(this::toResponse).toList()
        );
    }

    @Operation(summary = "Atualizar atribuição professor-turma-disciplina", description = "Modifica o professor, turma, disciplina ou parâmetros de avaliação da atribuição. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atribuição atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Atribuição não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherClassSubjectResponseDTO> update(
            @Parameter(description = "ID da atribuição") @PathVariable Integer id,
            @Valid @RequestBody TeacherClassSubjectRequestDTO dto) {
        TeacherClassSubject updated = teacherClassSubjectService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Excluir atribuição professor-turma-disciplina", description = "Remove a atribuição se não houver aulas ou avaliações associadas. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Atribuição excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir atribuição com aulas ou avaliações cadastradas"),
            @ApiResponse(responseCode = "404", description = "Atribuição não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da atribuição a ser excluída") @PathVariable Integer id) {
        teacherClassSubjectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private TeacherClassSubject toEntity(TeacherClassSubjectRequestDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setId(dto.teacherId());

        ClassRoom classRoom = new ClassRoom();
        classRoom.setId(dto.classRoomId());

        Subject subject = new Subject();
        subject.setId(dto.subjectId());

        TeacherClassSubject tcs = new TeacherClassSubject();
        tcs.setTeacher(teacher);
        tcs.setClassRoom(classRoom);
        tcs.setSubject(subject);
        tcs.setMinAssessmentsPerPeriod(dto.minAssessmentsPerPeriod());
        tcs.setMaxAssessmentsPerPeriod(dto.maxAssessmentsPerPeriod());
        return tcs;
    }

    private TeacherClassSubjectResponseDTO toResponse(TeacherClassSubject tcs) {
        return new TeacherClassSubjectResponseDTO(
                tcs.getId(),
                tcs.getTeacher().getId(),
                tcs.getTeacher().getName(),
                tcs.getClassRoom().getId(),
                tcs.getClassRoom().getName(),
                tcs.getSubject().getId(),
                tcs.getSubject().getName(),
                tcs.getMinAssessmentsPerPeriod(),
                tcs.getMaxAssessmentsPerPeriod(),
                tcs.getCreatedAt()
        );
    }
}
