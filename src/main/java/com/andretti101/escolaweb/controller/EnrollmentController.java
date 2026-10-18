package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.EnrollmentRequestDTO;
import com.andretti101.escolaweb.dto.response.EnrollmentResponseDTO;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.EnrollmentService;
import com.andretti101.escolaweb.service.PdfDocumentService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Matrículas", description = "Endpoints para gerenciamento de matrículas de estudantes em turmas, enturmações e transferências")
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthenticatedUserService authenticatedUserService;
    private final TeacherClassSubjectService teacherClassSubjectService;
    private final PdfDocumentService pdfDocumentService;

    @Operation(summary = "Realizar matrícula", description = "Matricula um estudante em uma turma específica. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Matrícula realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Estudante já matriculado em turma ativa ou dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> create(@Valid @RequestBody EnrollmentRequestDTO dto) {
        Enrollment created = enrollmentService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "Listar matrículas", description = "Retorna uma lista paginada de matrículas cadastradas com opção de filtrar apenas ativas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de matrículas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<org.springframework.data.domain.Page<EnrollmentResponseDTO>> findAll(
            @Parameter(description = "Filtrar apenas matrículas ativas") @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Enrollment> page = activeOnly
                ? enrollmentService.findAllActive(pageable)
                : enrollmentService.findAll(pageable);
        List<EnrollmentResponseDTO> dtoList = page.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar matrícula por ID", description = "Retorna os detalhes de uma matrícula específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matrícula encontrada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<EnrollmentResponseDTO> findById(
            @Parameter(description = "ID da matrícula") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(enrollmentService.findById(id)));
    }

    @Operation(summary = "Listar matrículas por estudante", description = "Retorna o histórico de todas as matrículas do estudante especificado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de matrículas retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado às matrículas de outro estudante"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<List<EnrollmentResponseDTO>> findByStudent(
            @Parameter(description = "ID do estudante") @PathVariable Integer studentId) {
        authenticatedUserService.enforceStudentOwnership(studentId);
        return ResponseEntity.ok(
                enrollmentService.findByStudent(studentId).stream().map(this::toResponse).toList()
        );
    }

    @Operation(summary = "Listar matrículas por turma", description = "Retorna todos os alunos matriculados em uma turma. Professores só acessam turmas em que lecionam; alunos só acessam a própria turma.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de matrículas da turma retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado para usuário não vinculado à turma"),
            @ApiResponse(responseCode = "404", description = "Turma não encontrada")
    })
    @GetMapping("/classroom/{classRoomId}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<EnrollmentResponseDTO>> findByClassRoom(
            @Parameter(description = "ID da turma") @PathVariable Integer classRoomId) {
        if (authenticatedUserService.isTeacher()) {
            com.andretti101.escolaweb.model.entity.Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
            boolean teachesInClassroom = teacherClassSubjectService.findByTeacher(teacher.getId())
                    .stream()
                    .filter(tcs -> tcs.getClassRoom().getAcademicYear().isActive())
                    .anyMatch(tcs -> tcs.getClassRoom().getId().equals(classRoomId));
            if (!teachesInClassroom) {
                return ResponseEntity.ok(java.util.List.of());
            }
        } else if (authenticatedUserService.isStudent()) {
            com.andretti101.escolaweb.model.entity.Student student = authenticatedUserService.getAuthenticatedStudent();
            boolean enrolledInClassroom = enrollmentService.findByStudent(student.getId())
                    .stream()
                    .anyMatch(e -> e.getClassRoom().getId().equals(classRoomId));
            if (!enrolledInClassroom) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }
        return ResponseEntity.ok(
                enrollmentService.findByClassRoom(classRoomId).stream().map(this::toResponse).toList()
        );
    }

    @Operation(summary = "Atualizar matrícula", description = "Atualiza os dados de uma matrícula existente. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matrícula atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> update(
            @Parameter(description = "ID da matrícula") @PathVariable Integer id,
            @Valid @RequestBody EnrollmentRequestDTO dto) {
        Enrollment updated = enrollmentService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Excluir matrícula", description = "Remove um registro de matrícula caso não possua notas ou presenças vinculadas. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Matrícula excluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir matrícula com notas ou faltas"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da matrícula a ser excluída") @PathVariable Integer id) {
        enrollmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar matrícula", description = "Reativa uma matrícula previamente desativada. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matrícula ativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> activate(
            @Parameter(description = "ID da matrícula") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(enrollmentService.activate(id)));
    }

    @Operation(summary = "Desativar matrícula", description = "Desativa a matrícula de um estudante na turma. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Matrícula desativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada")
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> deactivate(
            @Parameter(description = "ID da matrícula") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(enrollmentService.deactivate(id)));
    }

    @Operation(summary = "Transferir estudante para outra turma", description = "Transfere o estudante matriculado para uma nova turma, mantendo a consistência do histórico. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudante transferido de turma com sucesso"),
            @ApiResponse(responseCode = "400", description = "Turma de destino incompatível ou inválida"),
            @ApiResponse(responseCode = "404", description = "Matrícula ou nova turma não encontrada")
    })
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<EnrollmentResponseDTO> transfer(
            @Parameter(description = "ID da matrícula atual") @PathVariable Integer id,
            @Parameter(description = "ID da nova turma de destino") @RequestParam Integer newClassRoomId) {
        return ResponseEntity.ok(toResponse(enrollmentService.transferClassroom(id, newClassRoomId)));
    }

    @Operation(summary = "Exportar declaração de matrícula em PDF", description = "Gera a declaração oficial de matrícula escolar do estudante em formato PDF com código de verificação digital.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Declaração de matrícula em PDF gerada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (estudante só pode exportar sua própria declaração)"),
            @ApiResponse(responseCode = "404", description = "Matrícula não encontrada")
    })
    @GetMapping(value = "/{id}/certificate-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'STUDENT')")
    public ResponseEntity<byte[]> downloadCertificatePdf(
            @Parameter(description = "ID da matrícula") @PathVariable Integer id) {
        Enrollment enrollment = enrollmentService.findById(id);
        if (enrollment.getStudent() != null) {
            authenticatedUserService.enforceStudentOwnership(enrollment.getStudent().getId());
        }
        byte[] pdfBytes = pdfDocumentService.generateEnrollmentCertificatePdf(id);
        String regNum = enrollment.getStudent() != null && enrollment.getStudent().getRegistrationNumber() != null
                ? enrollment.getStudent().getRegistrationNumber()
                : String.valueOf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"declaracao-matricula-" + regNum + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ── Mapping

    private Enrollment toEntity(EnrollmentRequestDTO dto) {
        Student student = new Student();
        student.setId(dto.studentId());

        ClassRoom classRoom = new ClassRoom();
        classRoom.setId(dto.classRoomId());

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setClassRoom(classRoom);
        enrollment.setEnrollmentDate(dto.enrollmentDate());
        return enrollment;
    }

    private EnrollmentResponseDTO toResponse(Enrollment e) {
        return new EnrollmentResponseDTO(
                e.getId(),
                e.getStudent().getId(),
                e.getStudent().getName(),
                e.getStudent().getRegistrationNumber(),
                e.getClassRoom().getId(),
                e.getClassRoom().getName(),
                e.getClassRoom().getSchoolGrade() != null ? e.getClassRoom().getSchoolGrade().getLabel() : null,
                e.getClassRoom().getPeriodType(),
                e.getClassRoom().getPeriodType() != null ? e.getClassRoom().getPeriodType().getLabel() : null,
                e.getClassRoom() != null && e.getClassRoom().getAcademicYear() != null ? e.getClassRoom().getAcademicYear().getId() : null,
                e.getClassRoom().getAcademicYear() != null ? e.getClassRoom().getAcademicYear().getYear() : null,
                e.getEnrollmentDate(),
                e.isActive(),
                e.getGeneralSituation(),
                e.getCreatedAt()
        );
    }
}
