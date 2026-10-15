package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ChangePasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.ForgotPasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.LoginRequestDTO;
import com.andretti101.escolaweb.dto.request.RefreshTokenRequestDTO;
import com.andretti101.escolaweb.dto.request.ResetPasswordRequestDTO;
import com.andretti101.escolaweb.dto.response.AuthResponseDTO;
import com.andretti101.escolaweb.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@Tag(name = "Autenticação", description = "Endpoints para login, renovação de token, logout e recuperação de senha")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Autenticar usuário", description = "Valida as credenciais (e-mail e senha) e retorna os tokens JWT de acesso e refresh, além dos dados do usuário.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida"),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais incorretas ou conta inativa")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(summary = "Renovar token de acesso", description = "Gera um novo token JWT de acesso e um novo refresh token a partir de um refresh token válido.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados com sucesso"),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido ou formato incorreto"),
            @ApiResponse(responseCode = "401", description = "Refresh token expirado ou revogado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        return ResponseEntity.ok(authService.refresh(dto));
    }

    @Operation(summary = "Encerrar sessão (Logout)", description = "Revoga o refresh token informado, invalidando a sessão do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessão encerrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados do token inválidos")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        authService.logout(dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Alterar senha do usuário autenticado", description = "Permite que qualquer usuário logado altere sua própria senha informando a senha atual e a nova senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou nova senha inválida"),
            @ApiResponse(responseCode = "401", description = "Usuário não autenticado")
    })
    @PatchMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDTO dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(email, dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Solicitar recuperação de senha", description = "Envia um e-mail com o link e token para redefinição de senha caso o e-mail exista no sistema.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Instruções de redefinição enviadas caso o e-mail exista"),
            @ApiResponse(responseCode = "400", description = "E-mail inválido")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO dto) {
        authService.forgotPassword(dto);
        return ResponseEntity.ok(Map.of("message",
                "Se o e-mail informado estiver cadastrado, as instruções de redefinição foram enviadas."));
    }

    @Operation(summary = "Redefinir senha com token", description = "Redefine a senha do usuário utilizando o token de recuperação enviado por e-mail.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou formato de senha inválido")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO dto) {
        authService.resetPassword(dto);
        return ResponseEntity.noContent().build();
    }
}
