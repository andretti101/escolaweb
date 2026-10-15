package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.TeacherPatchRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherUpdateRequestDTO;
import com.andretti101.escolaweb.dto.response.SubjectResponseDTO;
import com.andretti101.escolaweb.dto.response.TeacherResponseDTO;
import com.andretti101.escolaweb.model.entity.Subject;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Professores", description = "Endpoints para gerenciamento do corpo docente, especialidades e habilitações")
@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "Cadastrar professor", description = "Cria um novo usuário com papel de Professor, definindo nome, e-mail, senha e disciplinas lecionadas. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Professor cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou e-mail já existente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> create(@Valid @RequestBody TeacherRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(teacherService.create(toEntity(dto))));
    }

    @Operation(summary = "Listar professores", description = "Retorna uma lista paginada de professores com opção de filtro por ativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de professores retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<org.springframework.data.domain.Page<TeacherResponseDTO>> findAll(
            @Parameter(description = "Filtrar apenas professores ativos") @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Teacher> page = activeOnly
                ? teacherService.findAllActive(pageable)
                : teacherService.findAll(pageable);
        List<TeacherResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar professor por ID", description = "Retorna os detalhes completos do professor, incluindo disciplinas que está habilitado a lecionar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<TeacherResponseDTO> findById(
            @Parameter(description = "ID do professor") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherService.findById(id)));
    }

    @Operation(summary = "Atualizar professor (completo)", description = "Atualiza todos os dados do professor (nome, e-mail, senha opcional e disciplinas). Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> update(
            @Parameter(description = "ID do professor") @PathVariable Integer id,
            @Valid @RequestBody TeacherUpdateRequestDTO dto) {
        return ResponseEntity.ok(toResponse(teacherService.update(id, toEntity(dto))));
    }

    @Operation(summary = "Atualizar dados parciais do professor (PATCH)", description = "Atualiza seletivamente campos do professor como nome, e-mail ou lista de disciplinas. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> patch(
            @Parameter(description = "ID do professor") @PathVariable Integer id,
            @Valid @RequestBody TeacherPatchRequestDTO dto) {
        return ResponseEntity.ok(toResponse(teacherService.patch(id, dto)));
    }

    @Operation(summary = "Excluir professor", description = "Exclui o cadastro do professor caso não existam vínculos impeditivos no diário de classe. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Professor excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir professor com aulas ou avaliações cadastradas"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do professor a ser excluído") @PathVariable Integer id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar professor", description = "Reativa o cadastro do professor permitindo seu login e alocação. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> activate(
            @Parameter(description = "ID do professor") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherService.activate(id)));
    }

    @Operation(summary = "Desativar professor", description = "Desativa o professor, impedindo novos acessos e alocações. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Professor desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<TeacherResponseDTO> deactivate(
            @Parameter(description = "ID do professor") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(teacherService.deactivate(id)));
    }

    // ── Mapping

    private Teacher toEntity(TeacherRequestDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setName(dto.name());
        teacher.setEmail(dto.email());
        teacher.setPassword(dto.password()); // encoded by TeacherServiceImpl.create()
        teacher.setSubjects(mapSubjectStubs(dto.subjectIds(), false));
        return teacher;
    }

    private Teacher toEntity(TeacherUpdateRequestDTO dto) {
        Teacher teacher = new Teacher();
        teacher.setName(dto.name());
        teacher.setEmail(dto.email());
        teacher.setPassword(dto.password());
        teacher.setSubjects(mapSubjectStubs(dto.subjectIds(), true));
        return teacher;
    }

    private Set<Subject> mapSubjectStubs(List<Integer> subjectIds, boolean preserveNull) {
        if (subjectIds == null) {
            return preserveNull ? null : new LinkedHashSet<>();
        }
        return subjectIds.stream()
                .filter(Objects::nonNull)
                .map(id -> {
                    Subject s = new Subject();
                    s.setId(id);
                    return s;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private TeacherResponseDTO toResponse(Teacher t) {
        List<SubjectResponseDTO> subjects = t.getSubjects() == null
                ? List.of()
                : t.getSubjects().stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(
                                (Subject s) -> s.getName() != null ? s.getName().toLowerCase() : "",
                                Comparator.naturalOrder()))
                        .map(s -> new SubjectResponseDTO(
                                s.getId(),
                                s.getName(),
                                s.getDescription(),
                                s.isActive(),
                                s.getCreatedAt()))
                        .toList();

        return new TeacherResponseDTO(
                t.getId(),
                t.getName(),
                t.getEmail(),
                t.isActive(),
                t.getCreatedAt(),
                subjects);
    }
}
