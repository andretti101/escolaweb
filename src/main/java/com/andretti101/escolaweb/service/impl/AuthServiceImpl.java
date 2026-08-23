package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.config.JwtTokenProvider;
import com.andretti101.escolaweb.dto.request.ChangePasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.ForgotPasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.LoginRequestDTO;
import com.andretti101.escolaweb.dto.request.RefreshTokenRequestDTO;
import com.andretti101.escolaweb.dto.request.ResetPasswordRequestDTO;
import com.andretti101.escolaweb.dto.response.AuthResponseDTO;
import com.andretti101.escolaweb.model.entity.PasswordResetToken;
import com.andretti101.escolaweb.model.entity.RefreshToken;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.PasswordResetTokenRepository;
import com.andretti101.escolaweb.service.AuthService;
import com.andretti101.escolaweb.service.EmailService;
import com.andretti101.escolaweb.service.RefreshTokenService;
import com.andretti101.escolaweb.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Value("${password-reset.expiration}")
    private long passwordResetExpiration;

    // ── Login / Refresh / Logout

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.email());
        User user = findUserByEmailOrThrow(dto.email());

        String accessToken = jwtTokenProvider.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.create(user);

        return buildResponse(accessToken, refreshToken.getToken(), userDetails);
    }

    @Override
    @Transactional
    public AuthResponseDTO refresh(RefreshTokenRequestDTO dto) {
        RefreshToken newRefreshToken = refreshTokenService.rotate(dto.refreshToken());

        UserDetails userDetails = userDetailsService
                .loadUserByUsername(newRefreshToken.getUser().getEmail());

        String accessToken = jwtTokenProvider.generateToken(userDetails);

        return buildResponse(accessToken, newRefreshToken.getToken(), userDetails);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDTO dto) {
        refreshTokenService.revoke(dto.refreshToken());
    }

    // ── Change Password ────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequestDTO dto) {
        User user = findUserByEmailOrThrow(email);

        userService.changePassword(user.getId(), dto.currentPassword(), dto.newPassword());

        refreshTokenService.revokeAllByUser(user);
    }

    // ── Forgot / Reset Password

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequestDTO dto) {
        Optional<User> optionalUser = userService.findByEmail(dto.email());

        if (optionalUser.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", dto.email());
            return;
        }

        User user = optionalUser.get();

        // Remove any existing reset tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(passwordResetExpiration / 1000);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expiresAt)
                .build();

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);

        log.info("Password reset token generated for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequestDTO dto) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(dto.token())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Token de redefinição inválido ou já utilizado."));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException(
                    "Token de redefinição expirado. Solicite um novo link.");
        }

        User user = resetToken.getUser();

        userService.resetPassword(user.getId(), dto.newPassword());

        refreshTokenService.revokeAllByUser(user);

        passwordResetTokenRepository.delete(resetToken);

        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    // ── Private helpers

    private AuthResponseDTO buildResponse(
            String accessToken, String refreshToken, UserDetails userDetails) {

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("UNKNOWN");

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                "Bearer",
                userDetails.getUsername(),
                role,
                jwtTokenProvider.getExpiration(),
                jwtTokenProvider.getRefreshExpiration());
    }

    private User findUserByEmailOrThrow(String email) {
        return userService.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado com o e-mail: " + email));
    }
}
