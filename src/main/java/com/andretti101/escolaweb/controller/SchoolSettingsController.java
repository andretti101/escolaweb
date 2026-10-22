package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.SchoolSettingsRequestDTO;
import com.andretti101.escolaweb.dto.response.SchoolSettingsResponseDTO;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.service.SchoolSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RestController;

// Controlador REST para consulta e atualização dos parâmetros institucionais da escola (endereço, contatos e cabeçalhos de documentos)
@Tag(name = "Configurações da Escola", description = "Endpoints para consulta e atualização dos dados cadastrais da instituição de ensino")
@RestController
@RequestMapping("/api/school-settings")
@RequiredArgsConstructor
public class SchoolSettingsController {

    private final SchoolSettingsService schoolSettingsService;

    @Operation(summary = "Obter configurações da escola", description = "Retorna os dados cadastrais da instituição (nome, endereço, telefone, e-mail) e critérios de aprovação vigentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configurações retornadas com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<SchoolSettingsResponseDTO> getSettings() {
        return ResponseEntity.ok(toResponse(schoolSettingsService.findSettings()));
    }

    @Operation(summary = "Atualizar configurações da escola", description = "Atualiza os dados de contato e endereço da instituição escolar. Exclusivo para Direção e Secretaria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configurações atualizadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @PutMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SchoolSettingsResponseDTO> update(@Valid @RequestBody SchoolSettingsRequestDTO dto) {
        SchoolSettings updated = schoolSettingsService.update(toEntity(dto));
        return ResponseEntity.ok(toResponse(updated));
    }

    // ── Mapping

    private SchoolSettings toEntity(SchoolSettingsRequestDTO dto) {
        SchoolSettings settings = new SchoolSettings();
        settings.setSchoolName(dto.schoolName());
        settings.setCep(dto.cep());
        settings.setAddress(dto.address());
        settings.setPhone(dto.phone());
        settings.setEmail(dto.email());
        // Sensitive fields (minimumGrade, minimumAttendance) are intentionally NOT mapped
        // They should only be updated by the AcademicYearConclusionService
        return settings;
    }

    private SchoolSettingsResponseDTO toResponse(SchoolSettings s) {
        return new SchoolSettingsResponseDTO(
                s.getId(),
                s.getSchoolName(),
                s.getCep(),
                s.getAddress(),
                s.getPhone(),
                s.getEmail(),
                s.getMinimumGrade(),
                s.getMinimumAttendance(),
                s.getCreatedAt()
        );
    }
}
