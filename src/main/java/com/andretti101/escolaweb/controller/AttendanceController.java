package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.AttendanceRequestDTO;
import com.andretti101.escolaweb.dto.response.AttendanceResponseDTO;
import com.andretti101.escolaweb.model.entity.Attendance;
import com.andretti101.escolaweb.model.entity.Lesson;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AttendanceService;
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

@Tag(name = "Frequência e Presença", description = "Endpoints para chamada escolar, registro de presenças/faltas e cálculo de frequência")
@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthenticatedUserService authenticatedUserService;
    private final LessonService lessonService;
    private final TeacherClassSubjectService teacherClassSubjectService;

    private final AcademicPeriodService academicPeriodService;

    @Operation(summary = "Registrar presença/falta", description = "Registra a situação de frequência de um estudante em uma aula (PRESENT, ABSENT, JUSTIFIED_ABSENCE).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Frequência registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos ou duplicados"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aula ou estudante não encontrado")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AttendanceResponseDTO> register(@Valid @RequestBody AttendanceRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Lesson lesson = lessonService.findById(dto.lessonId());
            authenticatedUserService.enforceTeacherOwnership(lesson.getTeacherClassSubject());
        }
        Attendance registered = attendanceService.register(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(registered));
    }

    @Operation(summary = "Listar registros de frequência", description = "Retorna uma lista paginada dos registros de frequência com enriquecimento de período acadêmico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de presenças retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<PagedResponse<AttendanceResponseDTO>> findAll(
            @org.springdoc.core.annotations.ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        final var cachedPeriods = academicPeriodService.findAll();
        Page<Attendance> page;
        if (authenticatedUserService.isTeacher()) {
            Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
            page = attendanceService.findByTeacherActiveAcademicYear(teacher.getId(), pageable);
        } else {
            page = attendanceService.findAll(pageable);
        }
        return ResponseEntity.ok(PagedResponse.of(
                page.getContent().stream().map(a -> toResponse(a, cachedPeriods)).toList(),
                page.getPageable(),
                page.getTotalElements()
        ));
    }

    @Operation(summary = "Buscar presença por ID", description = "Retorna o detalhamento de um registro de frequência específico. Estudantes só acessam suas próprias presenças.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Registro de frequência não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<AttendanceResponseDTO> findById(
            @Parameter(description = "ID do registro de frequência") @PathVariable Integer id) {
        Attendance attendance = attendanceService.findById(id);
        authenticatedUserService.enforceStudentOwnership(attendance.getStudent().getId());
        authenticatedUserService.enforceTeacherOwnership(attendance.getLesson().getTeacherClassSubject());
        return ResponseEntity.ok(toResponse(attendance));
    }

    @Operation(summary = "Listar presenças de uma aula", description = "Retorna a lista completa de chamada de uma determinada aula.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de frequência da aula retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Aula não encontrada")
    })
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<List<AttendanceResponseDTO>> findByLesson(
            @Parameter(description = "ID da aula") @PathVariable Integer lessonId) {
        if (authenticatedUserService.isTeacher()) {
            Lesson lesson = lessonService.findById(lessonId);
            authenticatedUserService.enforceTeacherOwnership(lesson.getTeacherClassSubject());
        }
        final var cachedPeriods = academicPeriodService.findAll();
        return ResponseEntity.ok(
                attendanceService.findByLesson(lessonId).stream().map(a -> toResponse(a, cachedPeriods)).toList()
        );
    }

    @Operation(summary = "Listar frequências por estudante", description = "Retorna todas as presenças e faltas de um estudante, com filtro opcional por ano letivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico de frequência retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<List<AttendanceResponseDTO>> findByStudent(
            @Parameter(description = "ID do estudante") @PathVariable Integer studentId,
            @Parameter(description = "ID do ano letivo opcional") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer yearId) {
        authenticatedUserService.enforceStudentOwnership(studentId);

        List<Attendance> attendances = attendanceService.findByStudent(studentId);

        if (yearId != null) {
            attendances = attendances.stream()
                    .filter(a -> a.getLesson() != null &&
                                 a.getLesson().getTeacherClassSubject() != null &&
                                 a.getLesson().getTeacherClassSubject().getClassRoom() != null &&
                                 a.getLesson().getTeacherClassSubject().getClassRoom().getAcademicYear() != null &&
                                 a.getLesson().getTeacherClassSubject().getClassRoom().getAcademicYear().getId().equals(yearId))
                    .toList();
        }

        if (authenticatedUserService.isTeacher()) {
            Set<Integer> myTcsIds = getAuthenticatedTeacherTcsIds();
            attendances = attendances.stream()
                    .filter(a -> myTcsIds.contains(a.getLesson().getTeacherClassSubject().getId()))
                    .toList();
        }

        final var cachedPeriods = academicPeriodService.findAll();
        return ResponseEntity.ok(attendances.stream().map(a -> toResponse(a, cachedPeriods)).toList());
    }

    @Operation(summary = "Calcular porcentagem de frequência do estudante", description = "Calcula a frequência percentual de um estudante na atribuição (disciplina da turma).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Frequência percentual calculada (ex: 85.50)"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Estudante ou atribuição não encontrada")
    })
    @GetMapping("/frequency/student/{studentId}/tcs/{tcsId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<BigDecimal> calculateFrequency(
            @Parameter(description = "ID do estudante") @PathVariable Integer studentId,
            @Parameter(description = "ID da atribuição professor-turma-disciplina") @PathVariable Integer tcsId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        if (authenticatedUserService.isTeacher()) {
            TeacherClassSubject tcs = teacherClassSubjectService.findById(tcsId);
            authenticatedUserService.enforceTeacherOwnership(tcs);
        }
        return ResponseEntity.ok(attendanceService.calculateFrequency(studentId, tcsId));
    }

    @Operation(summary = "Obter relatório consolidado de frequência do estudante", description = "Gera um relatório completo com total de aulas dadas, presenças, faltas justificadas, faltas não justificadas e percentual global.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório consolidado retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @GetMapping("/report/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<com.andretti101.escolaweb.dto.response.StudentAttendanceReportDTO> getStudentAttendanceReport(
            @Parameter(description = "ID do estudante") @PathVariable Integer studentId,
            @Parameter(description = "ID do ano letivo opcional") @org.springframework.web.bind.annotation.RequestParam(required = false) Integer yearId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        return ResponseEntity.ok(attendanceService.getStudentAttendanceReport(studentId, yearId));
    }

    @Operation(summary = "Atualizar status de frequência", description = "Altera o status de frequência de um registro (ex: de falta para falta justificada). Registra histórico de auditoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de frequência atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status inválido"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Registro de frequência não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<AttendanceResponseDTO> update(
            @Parameter(description = "ID do registro de frequência") @PathVariable Integer id,
            @Valid @RequestBody com.andretti101.escolaweb.dto.request.AttendanceUpdateRequestDTO dto) {
        if (authenticatedUserService.isTeacher()) {
            Attendance existing = attendanceService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(
                    existing.getLesson().getTeacherClassSubject());
        }
        
        Attendance incoming = new Attendance();
        if ("JUSTIFIED".equalsIgnoreCase(dto.status()) || "EXCUSED".equalsIgnoreCase(dto.status())) {
            incoming.setStatus(com.andretti101.escolaweb.model.enums.AttendanceStatus.JUSTIFIED_ABSENCE);
        } else {
            incoming.setStatus(com.andretti101.escolaweb.model.enums.AttendanceStatus.valueOf(dto.status().toUpperCase()));
        }

        Attendance updated = attendanceService.update(id, incoming);
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Excluir registro de frequência", description = "Remove um registro individual de presença/falta. Professores só removem registros de suas aulas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registro excluído com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Registro de frequência não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do registro de frequência a ser excluído") @PathVariable Integer id) {
        if (authenticatedUserService.isTeacher()) {
            Attendance existing = attendanceService.findById(id);
            authenticatedUserService.enforceTeacherOwnership(
                    existing.getLesson().getTeacherClassSubject());
        }
        attendanceService.delete(id);
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

    private Attendance toEntity(AttendanceRequestDTO dto) {
        Student student = new Student();
        student.setId(dto.studentId());

        Lesson lesson = new Lesson();
        lesson.setId(dto.lessonId());

        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setLesson(lesson);
        
        // Handle variations of frontend justified status
        if ("JUSTIFIED".equalsIgnoreCase(dto.status()) || "EXCUSED".equalsIgnoreCase(dto.status())) {
            attendance.setStatus(com.andretti101.escolaweb.model.enums.AttendanceStatus.JUSTIFIED_ABSENCE);
        } else {
            attendance.setStatus(com.andretti101.escolaweb.model.enums.AttendanceStatus.valueOf(dto.status().toUpperCase()));
        }
        
        return attendance;
    }

    private AttendanceResponseDTO toResponse(Attendance a) {
        return toResponse(a, null);
    }

    private AttendanceResponseDTO toResponse(Attendance a, List<com.andretti101.escolaweb.model.entity.AcademicPeriod> cachedPeriods) {
        String pName = "-";
        java.time.LocalDate d = a.getLesson().getLessonDate();
        if (d != null) {
            var periods = cachedPeriods != null ? cachedPeriods : academicPeriodService.findAll();
            for (var p : periods) {
                if (p.getStartDate() != null && p.getEndDate() != null) {
                    if (!d.isBefore(p.getStartDate()) && !d.isAfter(p.getEndDate())) {
                        pName = p.getName();
                        break;
                    }
                }
            }
        }

        return new AttendanceResponseDTO(
                a.getId(),
                a.getStudent().getId(),
                a.getStudent().getName(),
                a.getLesson().getId(),
                a.getLesson().getLessonDate(),
                a.getLesson().getTeacherClassSubject().getId(),
                a.getLesson().getTeacherClassSubject().getSubject().getName(),
                pName,
                a.getLesson().getLessonCount() != null ? a.getLesson().getLessonCount().getValue() : 1,
                a.getStatus(),
                a.getLesson().getContent(),
                a.getLesson().getNotes(),
                a.getCreatedAt()
        );
    }
}
