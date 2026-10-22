package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.StudentPatchRequestDTO;
import com.andretti101.escolaweb.dto.request.StudentRequestDTO;
import com.andretti101.escolaweb.dto.request.StudentUpdateRequestDTO;
import com.andretti101.escolaweb.dto.response.StudentResponseDTO;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.StudentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Controlador REST para cadastro discente, dados de contato dos responsáveis e consulta de perfil de alunos
@Tag(name = "Estudantes", description = "Endpoints para gerenciamento do cadastro de alunos, dados de contato e responsáveis")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final com.andretti101.escolaweb.service.ReportCardService reportCardService;
    private final com.andretti101.escolaweb.service.AcademicYearService academicYearService;
    private final AuthenticatedUserService authenticatedUserService;

    @Operation(summary = "Cadastrar estudante", description = "Cria a ficha cadastral do estudante com dados pessoais, matrícula, data de nascimento e contatos. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estudante cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos, e-mail ou matrícula já existentes"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PostMapping
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> create(@Valid @RequestBody StudentRequestDTO dto) {
        Student created = studentService.create(toEntity(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "Listar estudantes", description = "Retorna uma lista paginada de estudantes com suporte a filtros por status ativo e alunos não matriculados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de estudantes retornada com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL')")
    public ResponseEntity<org.springframework.data.domain.Page<StudentResponseDTO>> findAll(
            @Parameter(description = "Filtrar apenas estudantes ativos") @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @Parameter(description = "Filtrar apenas estudantes sem matrícula ativa") @RequestParam(required = false, defaultValue = "false") boolean unenrolledOnly,
            @org.springdoc.core.annotations.ParameterObject org.springframework.data.domain.Pageable pageable) {
        
        org.springframework.data.domain.Page<Student> page;
        if (unenrolledOnly) {
            page = studentService.findAllActiveUnenrolled(pageable);
        } else if (activeOnly) {
            page = studentService.findAllActive(pageable);
        } else {
            page = studentService.findAll(pageable);
        }
        
        List<Student> students = page.getContent();
        java.util.Map<Integer, String> situationMap = new java.util.HashMap<>();
        try {
            com.andretti101.escolaweb.model.entity.AcademicYear activeYear = academicYearService.findActive();
            if (activeYear != null) {
                List<com.andretti101.escolaweb.model.entity.Enrollment> activeEnrollments = new java.util.ArrayList<>();
                for (Student s : students) {
                    if (s.getEnrollments() != null) {
                        for (com.andretti101.escolaweb.model.entity.Enrollment e : s.getEnrollments()) {
                            if (e.isActive() && e.getClassRoom().getAcademicYear().getId().equals(activeYear.getId())) {
                                activeEnrollments.add(e);
                                break;
                            }
                        }
                    }
                }
                if (!activeEnrollments.isEmpty()) {
                    List<com.andretti101.escolaweb.dto.response.ReportCardDTO> reports = reportCardService.generateReportCardsBatch(activeEnrollments, activeYear);
                    for (com.andretti101.escolaweb.dto.response.ReportCardDTO r : reports) {
                        situationMap.put(r.studentId(), r.generalSituation().getLabel());
                    }
                }
            }
        } catch(Exception ex) {
            // ignore
        }
        
        List<StudentResponseDTO> dtoList = students.stream().map(s -> toResponse(s, situationMap)).toList();
        return ResponseEntity.ok(com.andretti101.escolaweb.dto.response.PagedResponse.of(dtoList, page.getPageable(), page.getTotalElements()));
    }

    @Operation(summary = "Buscar estudante por ID", description = "Retorna os dados cadastrais do estudante. O aluno só pode consultar seu próprio perfil.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudante encontrado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado ao perfil de outro estudante"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETARY', 'PRINCIPAL', 'STUDENT')")
    public ResponseEntity<StudentResponseDTO> findById(
            @Parameter(description = "ID do estudante") @PathVariable Integer id) {
        authenticatedUserService.enforceStudentOwnership(id);
        Student s = studentService.findById(id);
        java.util.Map<Integer, String> situationMap = new java.util.HashMap<>();
        try {
            com.andretti101.escolaweb.model.entity.AcademicYear activeYear = academicYearService.findActive();
            if (activeYear != null && s.getEnrollments() != null) {
                for (com.andretti101.escolaweb.model.entity.Enrollment e : s.getEnrollments()) {
                    if (e.isActive() && e.getClassRoom().getAcademicYear().getId().equals(activeYear.getId())) {
                        var rc = reportCardService.generateReportCard(s.getId(), activeYear.getId());
                        if (rc != null) {
                            situationMap.put(s.getId(), rc.generalSituation().getLabel());
                        }
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok(toResponse(s, situationMap));
    }

    @Operation(summary = "Atualizar estudante (completo)", description = "Atualiza todos os dados cadastrais do estudante (nome, e-mail, senha opcional, telefones, matrícula, data de nascimento). Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudante atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> update(
            @Parameter(description = "ID do estudante") @PathVariable Integer id,
            @Valid @RequestBody StudentUpdateRequestDTO dto) {
        Student updated = studentService.update(id, toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Atualizar dados parciais do estudante (PATCH)", description = "Permite atualizar seletivamente campos como telefone próprio ou telefone dos responsáveis. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudante atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> patch(
            @Parameter(description = "ID do estudante") @PathVariable Integer id,
            @Valid @RequestBody StudentPatchRequestDTO dto) {
        Student updated = studentService.patch(id, dto);
        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(summary = "Excluir estudante", description = "Exclui o estudante caso não possua matrículas ou histórico acadêmico vinculado. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Estudante excluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não é possível excluir estudante com matrículas ou histórico"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID do estudante a ser excluído") @PathVariable Integer id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ativar estudante", description = "Reativa a conta do estudante. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudante ativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> activate(
            @Parameter(description = "ID do estudante") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(studentService.activate(id)));
    }

    @Operation(summary = "Desativar estudante", description = "Desativa o estudante no sistema. Exclusivo para Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estudante desativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Estudante não encontrado")
    })
    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SECRETARY')")
    public ResponseEntity<StudentResponseDTO> deactivate(
            @Parameter(description = "ID do estudante") @PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(studentService.deactivate(id)));
    }

    //  Mapping

    private Student toEntity(StudentRequestDTO dto) {
        Student student = new Student();
        student.setName(dto.name());
        student.setEmail(dto.email());
        student.setPassword(dto.password());
        student.setOwnPhone(dto.ownPhone());
        student.setGuardianPhone(dto.guardianPhone());
        student.setRegistrationNumber(dto.registrationNumber());
        student.setBirthDate(dto.birthDate());
        return student;
    }

    private Student toEntity(StudentUpdateRequestDTO dto) {
        Student student = new Student();
        student.setName(dto.name());
        student.setEmail(dto.email());
        student.setPassword(dto.password());
        student.setOwnPhone(dto.ownPhone());
        student.setGuardianPhone(dto.guardianPhone());
        student.setRegistrationNumber(dto.registrationNumber());
        student.setBirthDate(dto.birthDate());
        return student;
    }

    private StudentResponseDTO toResponse(Student s) {
        return toResponse(s, new java.util.HashMap<>());
    }

    private StudentResponseDTO toResponse(Student s, java.util.Map<Integer, String> situationMap) {
        String className = "-";
        String situation = "Pendente";
        if (s.getEnrollments() != null) {
            try {
                var activeEnrollment = s.getEnrollments().stream()
                        .filter(com.andretti101.escolaweb.model.entity.Enrollment::isActive)
                        .findFirst();
                if (activeEnrollment.isPresent()) {
                    className = activeEnrollment.get().getClassRoom().getName();
                    if (situationMap.containsKey(s.getId())) {
                        situation = situationMap.get(s.getId());
                    } else if (activeEnrollment.get().getGeneralSituation() != null) {
                        situation = activeEnrollment.get().getGeneralSituation().getLabel();
                    }
                }
            } catch (Exception ignored) {}
        }

        return new StudentResponseDTO(
                s.getId(),
                s.getName(),
                s.getEmail(),
                s.getOwnPhone(),
                s.getGuardianPhone(),
                s.getRegistrationNumber(),
                s.getBirthDate(),
                s.isActive(),
                s.getCreatedAt(),
                className,
                situation
        );
    }
}