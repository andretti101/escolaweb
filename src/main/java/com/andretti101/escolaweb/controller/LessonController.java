package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.LessonRequestDTO;
import com.andretti101.escolaweb.dto.response.LessonResponseDTO;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.LessonCount;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.LessonService;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Aulas", description = "Endpoints para registro de diário de classe, planejamento de aulas e conteúdos ministrados")
@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final AuthenticatedUserService authenticatedUserService;
    private final TeacherClassSubjectService teacherClassSubjectService;

    @Operation(summary = "Registrar aula no diário", description = "Cadastra uma aula ministrada informando data, conteúdo programático, observações e quantidade de tempos de aula. Professores só registram aulas em suas atribuições.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Aula registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da aula inválidos ou limite de aulas excedido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<LessonResponseDTO> create(@Valid @RequestBody LessonRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(dto.teacherClassSubjectId());
            authenticatedUserService.enforceTeacherOwnership(tcs);
        }
        Lesson created = lessonService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "Listar aulas", description = "Retorna uma lista paginada de aulas registradas. Para professores, filtra automaticamente as aulas de suas turmas no ano letivo ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de aulas retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.PagedResponse<LessonResponseDTO>> findAll(
            @org.springdoc.core.annotations.ParameterObject @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        if (authenticatedUserService.isTeacher()) {
            Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
            org.springframework.data.domain.Page<Lesson> page = lessonService.findByTeacherActiveAcademicYear(teacher.getId(), pageable);
            return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(
                    page.getContent().stream().map(this::toResponse).toList(),
                    page.getPageable(),
                    page.getTotalElements()
            ));
        }
        org.springframework.data.domain.Page<Lesson> page = lessonService.findAll(pageable);
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getPageable(),
                page.getTotalElements()
        ));
    }

    @Operation(summary = "Buscar aula por ID", description = "Retorna os detalhes de uma aula específica pelo ID. Professores só acessam aulas de suas atribuições.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aula encontrada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<LessonResponseDTO> findById(
            @Parameter(description = "ID da aula") @PathVariable Integer id) {
        Lesson lesson = lessonService.findById(id);
        authenticatedUserService.enforceTeacherOwnership(lesson.getTeacherClassSubject());
        return ResponseEntity.ok(toResponse(lesson));
    }

    @Operation(summary = "Listar aulas por atribuição", description = "Retorna todas as aulas lecionadas para uma determinada atribuição professor-turma-disciplina.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de aulas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Atribuição não encontrada")
    })
    @GetMapping("/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<LessonResponseDTO>> findByTeacherClassSubject(
            @Parameter(description = "ID da atribuição professor-turma-disciplina") @PathVariable Integer tcsId) {
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(tcsId);
            authenticatedUserService.enforceTeacherOwnership(tcs);
            if (!tcs.getClassRoom().getAcademicYear().isActive()) {
                return ResponseEntity.ok(java.util.List.of());
            }
        }
        return ResponseEntity.ok(
                lessonService.findByTeacherClassSubject(tcsId).stream().map(this::toResponse).toList()
        );
    }

    @Operation(summary = "Atualizar aula", description = "Atualiza o conteúdo, data ou observações de uma aula ministrada. Professores só atualizam suas próprias aulas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aula atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<LessonResponseDTO> update(
            @Parameter(description = "ID da aula") @PathVariable Integer id,
            @Valid @RequestBody LessonRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Lesson existing = lessonService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(existing.getTeacherClassSubject());
            TeacherClassSubject newTcs = teacherClassSubjectService.findById(dto.teacherClassSubjectId());
            authenticatedUserService.enforceTeacherOwnership(newTcs);
        }
        Lesson updated = lessonService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Excluir aula", description = "Exclui o registro de uma aula e suas listas de frequência associadas. Professores só excluem suas próprias aulas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Aula excluída com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da aula a ser excluída") @PathVariable Integer id) {
        if (authenticatedUserService.isTeacher()) {
            Lesson existing = lessonService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(existing.getTeacherClassSubject());
        }
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private Lesson toEntity(LessonRequestDTO dto) {
        TeacherClassSubject tcs = new TeacherClassSubject();
        tcs.setId(dto.teacherClassSubjectId());

        Lesson lesson = new Lesson();
        lesson.setLessonDate(dto.lessonDate());
        lesson.setContent(dto.content());
        lesson.setNotes(dto.notes());
        lesson.setTeacherClassSubject(tcs);

        lesson.setLessonCount(
                dto.lessonCount() != null
                        ? LessonCount.fromValue(dto.lessonCount())
                        : LessonCount.ONE
        );

        return lesson;
    }

    private LessonResponseDTO toResponse(Lesson l) {
        return new LessonResponseDTO(
                l.getId(),
                l.getLessonDate(),
                l.getContent(),
                l.getNotes(),
                l.getTeacherClassSubject().getId(),
                l.getTeacherClassSubject().getSubject().getName(),
                l.getTeacherClassSubject().getTeacher().getName(),
                l.getTeacherClassSubject().getClassRoom().getName(),
                l.getLessonCount().getValue(),
                l.getCreatedAt(),
                l.getUpdatedAt()
        );
    }
}
