package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AnnouncementRequestDTO;
import com.andretti101.escolaweb.dto.response.AnnouncementResponseDTO;
import com.andretti101.escolaweb.dto.response.ClassRoomSimpleDTO;
import com.andretti101.escolaweb.dto.response.PagedResponse;
import com.andretti101.escolaweb.model.entity.Announcement;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.service.AnnouncementService;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Controlador REST para comunicados e murais escolares direcionados por turma ou corpo docente
@Tag(name = "Comunicados e Avisos", description = "Endpoints para emissão de murais de avisos, comunicados por turma, comunicados para professores e notificações de leitura")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AuthenticatedUserService authenticatedUserService;

    // Publica um novo comunicado associando-o opcionalmente a turmas ou aos docentes
    @Operation(summary = "Criar comunicado escolar", description = "Publica um novo comunicado podendo direcioná-lo a turmas específicas ou a todos os professores.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comunicado criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<AnnouncementResponseDTO> create(@Valid @RequestBody AnnouncementRequestDTO dto) {
        Announcement announcement = toEntity(dto);
        Announcement created = announcementService.create(announcement, dto.classRoomIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "Listar todos os comunicados ativos (com filtros)", description = "Retorna uma lista paginada de avisos ativos com filtros combinados por autor, turma, ano letivo e público-alvo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de comunicados retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<org.springframework.data.domain.Page<AnnouncementResponseDTO>> findAll(
            @Parameter(description = "ID do autor do comunicado") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer authorId,
            @Parameter(description = "ID da turma vinculada") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer classRoomId,
            @Parameter(description = "ID do ano letivo") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer academicYearId,
            @Parameter(description = "Filtro para comunicados voltados a professores") @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean targetTeachers,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<com.andretti101.escolaweb.model.entity.Announcement> page = announcementService.findWithFilters(authorId, classRoomId, academicYearId, targetTeachers, pageable);
        return ResponseEntity.ok(PagedResponse.of(page.map(this::toResponse)));
    }

    @Operation(summary = "Listar comunicados arquivados (com filtros)", description = "Retorna uma lista paginada de comunicados arquivados com opções de filtros.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de comunicados arquivados retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/archived")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<org.springframework.data.domain.Page<AnnouncementResponseDTO>> findArchived(
            @Parameter(description = "ID do autor do comunicado") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer authorId,
            @Parameter(description = "ID da turma vinculada") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer classRoomId,
            @Parameter(description = "ID do ano letivo") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer academicYearId,
            @Parameter(description = "Filtro para comunicados voltados a professores") @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean targetTeachers,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<com.andretti101.escolaweb.model.entity.Announcement> page = announcementService.findArchivedWithFilters(authorId, classRoomId, academicYearId, targetTeachers, pageable);
        return ResponseEntity.ok(PagedResponse.of(page.map(this::toResponse)));
    }

    @Operation(summary = "Buscar comunicado por ID", description = "Retorna os detalhes de um aviso específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comunicado encontrado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<AnnouncementResponseDTO> findById(
            @Parameter(description = "ID do comunicado") @PathVariable Integer id) {
        Announcement announcement = announcementService.findById(id);
        return ResponseEntity.ok(toResponse(announcement));
    }

    @Operation(summary = "Listar meus comunicados emitidos", description = "Retorna todos os comunicados criados pelo usuário logado (Professor, Secretaria ou Direção).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de comunicados retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/my-announcements")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findByAuthor() {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        List<Announcement> announcements = announcementService.findByAuthorId(userId);
        return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Listar comunicados das turmas lecionadas", description = "Retorna os avisos direcionados às turmas em que o professor autenticado leciona.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de comunicados retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso exclusivo para Professores")
    })
    @GetMapping("/my-teaching-classrooms")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findByTeacherClassRooms() {
        try {
            Integer teacherId = authenticatedUserService.getAuthenticatedUser().getId();
            List<Announcement> announcements = announcementService.findByTeacherClassRooms(teacherId);
            return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Operation(summary = "Listar comunicados gerais para professores", description = "Retorna os avisos da escola direcionados exclusivamente ao corpo docente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de comunicados retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso exclusivo para Professores")
    })
    @GetMapping("/for-teachers")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findForTeachers() {
        List<Announcement> announcements = announcementService.findForTeachers();
        return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Listar comunicados da minha turma (Aluno)", description = "Retorna os avisos enviados para a turma atual do estudante autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de comunicados da turma retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso exclusivo para Estudantes")
    })
    @GetMapping("/my-classroom")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AnnouncementResponseDTO>> findByStudentClassRooms() {
        Integer studentId = authenticatedUserService.getAuthenticatedStudent().getId();
        List<Announcement> announcements = announcementService.findByStudentClassRooms(studentId);
        return ResponseEntity.ok(announcements.stream().map(this::toResponse).toList());
    }

    @Operation(summary = "Atualizar comunicado", description = "Altera o título, mensagem, data de evento ou turmas de um aviso. Usuários só editam seus próprios comunicados (exceto admins).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comunicado atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<AnnouncementResponseDTO> update(
            @Parameter(description = "ID do comunicado") @PathVariable Integer id,
            @Valid @RequestBody AnnouncementRequestDTO dto) {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        Announcement updated = toEntity(dto);
        Announcement result = announcementService.update(id, updated, dto.classRoomIds(), userId);
        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(summary = "Excluir comunicado", description = "Remove um comunicado. Usuários só excluem seus próprios comunicados (exceto admins).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Comunicado excluído com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Comunicado não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do comunicado a ser excluído") @PathVariable Integer id) {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        announcementService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Mapping

    private Announcement toEntity(AnnouncementRequestDTO dto) {
        Announcement announcement = new Announcement();
        announcement.setTitle(dto.title());
        announcement.setMessage(dto.message());
        announcement.setEventDate(dto.eventDate());
        announcement.setTargetTeachers(dto.targetTeachers() != null ? dto.targetTeachers() : false);
        return announcement;
    }

    private AnnouncementResponseDTO toResponse(Announcement a) {
        List<ClassRoomSimpleDTO> classRoomDtos = a.getClassRooms().stream()
                .map(this::toClassRoomSimple)
                .toList();

        return new AnnouncementResponseDTO(
                a.getId(),
                a.getTitle(),
                a.getMessage(),
                a.getEventDate(),
                a.getCreatedAt(),
                a.getAuthor().getId(),
                a.getAuthor().getName(),
                a.getAuthor().getRole() != null ? a.getAuthor().getRole().getLabel() : "Desconhecido",
                classRoomDtos,
                a.getTargetTeachers() != null ? a.getTargetTeachers() : false
        );
    }

    private ClassRoomSimpleDTO toClassRoomSimple(ClassRoom c) {
        return new ClassRoomSimpleDTO(
                c.getId(),
                c.getName(),
                c.getPeriodType(),
                c.getPeriodType() != null ? c.getPeriodType().getLabel() : null
        );
    }

    @Operation(summary = "Verificar comunicados não lidos", description = "Verifica se existem comunicados novos ou não lidos pelo usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de leitura retornado com sucesso (true/false)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/has-unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> hasUnreadAnnouncements() {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        return ResponseEntity.ok(announcementService.hasUnreadAnnouncements(userId));
    }

    @Operation(summary = "Marcar comunicados como lidos", description = "Marca todos os comunicados pendentes como lidos para o usuário autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comunicados marcados como lidos com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @PostMapping("/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead() {
        Integer userId = authenticatedUserService.getAuthenticatedUser().getId();
        announcementService.markAnnouncementsAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
