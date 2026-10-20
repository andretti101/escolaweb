package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.TimetableSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.TimetableSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyScheduleDTO;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Grade Horária", description = "Endpoints para consulta e gestão da grade horária semanal de aulas")
@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;
    private final AuthenticatedUserService authenticatedUserService;

    @Operation(summary = "Consultar grade horária da turma", description = "Retorna os horários semanais da turma informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade da turma retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @GetMapping("/classroom/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<WeeklyScheduleDTO> getScheduleByClassroom(
            @Parameter(description = "ID da turma") @PathVariable Integer id) {
        return ResponseEntity.ok(timetableService.getScheduleByClassroom(id));
    }

    @Operation(summary = "Consultar minha grade horária", description = "Retorna a grade do aluno autenticado ou professor autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade do usuário retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso restrito a professores e alunos")
    })
    @GetMapping("/my-schedule")
    @PreAuthorize("hasAnyRole('TEACHER', 'STUDENT')")
    public ResponseEntity<WeeklyScheduleDTO> getMySchedule(
            @Parameter(description = "ID do ano letivo (opcional, padrão: ano letivo ativo)")
            @RequestParam(required = false) Integer academicYearId) {
        if (authenticatedUserService.isStudent()) {
            return ResponseEntity.ok(timetableService.getScheduleForAuthenticatedStudent());
        }
        return ResponseEntity.ok(timetableService.getScheduleForAuthenticatedTeacher(academicYearId));
    }

    @Operation(summary = "Consultar grade de um professor", description = "Retorna a grade horária semanal de um professor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade do professor retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Professor não encontrado")
    })
    @GetMapping("/teacher/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<WeeklyScheduleDTO> getScheduleByTeacher(
            @Parameter(description = "ID do professor") @PathVariable Integer id,
            @Parameter(description = "ID do ano letivo (opcional, padrão: ano letivo ativo)")
            @RequestParam(required = false) Integer academicYearId) {
        return ResponseEntity.ok(timetableService.getScheduleByTeacher(id, academicYearId));
    }

    @Operation(summary = "Adicionar horário de aula", description = "Cadastra um período na grade horária. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Horário adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas Direção pode gerenciar grade horária"),
            @ApiResponse(responseCode = "409", description = "Choque de horário com outra aula do professor ou da turma")
    })
    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<TimetableSlotResponseDTO> create(@Valid @RequestBody TimetableSlotRequestDTO dto) {
        TimetableSlotResponseDTO response = timetableService.addSlot(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Atualizar horário de aula", description = "Modifica um período existente na grade horária. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horário atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas Direção pode gerenciar grade horária"),
            @ApiResponse(responseCode = "404", description = "Horário de aula não encontrado"),
            @ApiResponse(responseCode = "409", description = "Choque de horário com outra aula do professor ou da turma")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<TimetableSlotResponseDTO> update(
            @Parameter(description = "ID do horário") @PathVariable Integer id,
            @Valid @RequestBody TimetableSlotRequestDTO dto) {
        TimetableSlotResponseDTO response = timetableService.updateSlot(id, dto);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Remover horário de aula", description = "Exclui um período da grade horária. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Horário removido com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas Direção pode gerenciar grade horária"),
            @ApiResponse(responseCode = "404", description = "Horário de aula não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do horário") @PathVariable Integer id) {
        timetableService.removeSlot(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Geração automática da grade horária", description = "Gera a grade horária semanal sem conflitos para as turmas ativas do ano letivo com base na carga horária semanal das disciplinas. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grade horária gerada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas Direção pode gerenciar grade horária"),
            @ApiResponse(responseCode = "409", description = "Choque de horários insolúvel com a carga docente atual")
    })
    @PostMapping("/auto-generate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.TimetableGenerationResponseDTO> autoGenerate(
            @Valid @RequestBody com.andretti101.escolaweb.dto.request.TimetableGenerationRequestDTO dto) {
        return ResponseEntity.ok(timetableService.generateTimetable(dto));
    }
}
