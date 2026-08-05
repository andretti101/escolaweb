package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.SecretaryRequestDTO;
import com.andretti101.escolaweb.dto.response.SecretaryResponseDTO;
import com.andretti101.escolaweb.model.entity.Secretary;
import com.andretti101.escolaweb.service.SecretaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secretaries")
@RequiredArgsConstructor
public class SecretaryController {

    private final SecretaryService secretaryService;

    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<SecretaryResponseDTO> create(@Valid @RequestBody SecretaryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(secretaryService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<List<SecretaryResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Secretary> secretaries = activeOnly
                ? secretaryService.findAllActive()
                : secretaryService.findAll();
        return ResponseEntity.ok(secretaries.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<SecretaryResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(secretaryService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<SecretaryResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody SecretaryRequestDTO dto) {
        return ResponseEntity.ok(toResponse(secretaryService.update(id, toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        secretaryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<SecretaryResponseDTO> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(secretaryService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<SecretaryResponseDTO> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(secretaryService.deactivate(id)));
    }

    // ── Mapping

    private Secretary toEntity(SecretaryRequestDTO dto) {
        Secretary secretary = new Secretary();
        secretary.setName(dto.name());
        secretary.setEmail(dto.email());
        secretary.setPassword(dto.password());
        return secretary;
    }

    private SecretaryResponseDTO toResponse(Secretary s) {
        return new SecretaryResponseDTO(
                s.getId(),
                s.getName(),
                s.getEmail(),
                s.isActive(),
                s.getCreatedAt());
    }
}
