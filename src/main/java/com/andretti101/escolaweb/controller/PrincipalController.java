package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.PrincipalRequestDTO;
import com.andretti101.escolaweb.dto.response.PrincipalResponseDTO;
import com.andretti101.escolaweb.model.entity.Principal;
import com.andretti101.escolaweb.service.PrincipalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/principals")
@RequiredArgsConstructor
public class PrincipalController {

    private final PrincipalService principalService;

    @PostMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<PrincipalResponseDTO> create(@Valid @RequestBody PrincipalRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(principalService.create(toEntity(dto))));
    }

    @GetMapping
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<List<PrincipalResponseDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        List<Principal> principals = activeOnly
                ? principalService.findAllActive()
                : principalService.findAll();
        return ResponseEntity.ok(principals.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRINCIPAL', 'SECRETARY')")
    public ResponseEntity<PrincipalResponseDTO> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(principalService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<PrincipalResponseDTO> update(
            @PathVariable Integer id, @Valid @RequestBody PrincipalRequestDTO dto) {
        return ResponseEntity.ok(toResponse(principalService.update(id, toEntity(dto))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        principalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<PrincipalResponseDTO> activate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(principalService.activate(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PRINCIPAL')")
    public ResponseEntity<PrincipalResponseDTO> deactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(principalService.deactivate(id)));
    }

    // ── Mapping

    private Principal toEntity(PrincipalRequestDTO dto) {
        Principal principal = new Principal();
        principal.setName(dto.name());
        principal.setEmail(dto.email());
        principal.setPassword(dto.password());
        return principal;
    }

    private PrincipalResponseDTO toResponse(Principal p) {
        return new PrincipalResponseDTO(
                p.getId(),
                p.getName(),
                p.getEmail(),
                p.isActive(),
                p.getCreatedAt());
    }
}
