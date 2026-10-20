package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.MealMenuSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.MealMenuSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyMealMenuDTO;
import com.andretti101.escolaweb.service.MealMenuService;
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

@Tag(name = "Cardápio Escolar", description = "Endpoints para consulta e gestão da grade do cardápio escolar semanal")
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MealMenuController {

    private final MealMenuService mealMenuService;

    @Operation(summary = "Consultar cardápio escolar semanal", description = "Retorna a grade de refeições de toda a semana para o ano letivo ativo ou informado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cardápio semanal retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ano letivo não encontrado")
    })
    @GetMapping("/weekly")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<WeeklyMealMenuDTO> getWeeklyMenu(
            @Parameter(description = "ID do ano letivo (opcional, padrão: ano letivo ativo)")
            @RequestParam(required = false) Integer academicYearId) {
        return ResponseEntity.ok(mealMenuService.getWeeklyMenu(academicYearId));
    }

    @Operation(summary = "Consultar item do cardápio", description = "Retorna os detalhes de uma refeição cadastrada pelo seu ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item do cardápio retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Refeição não encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<MealMenuSlotResponseDTO> getById(
            @Parameter(description = "ID da refeição") @PathVariable Integer id) {
        return ResponseEntity.ok(mealMenuService.getSlotById(id));
    }

    @Operation(summary = "Cadastrar refeição no cardápio", description = "Cadastra uma refeição em um dia e período da semana. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Refeição cadastrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas Direção pode gerenciar o cardápio"),
            @ApiResponse(responseCode = "409", description = "Já existe refeição cadastrada para este dia e período")
    })
    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<MealMenuSlotResponseDTO> create(@Valid @RequestBody MealMenuSlotRequestDTO dto) {
        MealMenuSlotResponseDTO created = mealMenuService.createSlot(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Atualizar refeição do cardápio", description = "Modifica uma refeição existente no cardápio escolar. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refeição atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas Direção pode gerenciar o cardápio"),
            @ApiResponse(responseCode = "404", description = "Refeição não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito com outra refeição cadastrada no mesmo dia e período")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<MealMenuSlotResponseDTO> update(
            @Parameter(description = "ID da refeição") @PathVariable Integer id,
            @Valid @RequestBody MealMenuSlotRequestDTO dto) {
        MealMenuSlotResponseDTO updated = mealMenuService.updateSlot(id, dto);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Remover refeição do cardápio", description = "Exclui uma refeição cadastrada no cardápio escolar. Exclusivo para Direção.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Refeição removida com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado - apenas Direção pode gerenciar o cardápio"),
            @ApiResponse(responseCode = "404", description = "Refeição não encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da refeição") @PathVariable Integer id) {
        mealMenuService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}
