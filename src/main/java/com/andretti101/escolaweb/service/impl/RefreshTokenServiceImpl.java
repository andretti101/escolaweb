package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.config.JwtTokenProvider;
import com.andretti101.escolaweb.model.entity.RefreshToken;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.RefreshTokenRepository;
import com.andretti101.escolaweb.service.RefreshTokenService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public RefreshToken create(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .revoked(false)
                .expiresAt(LocalDateTime.now()
                        .plus(jwtTokenProvider.getRefreshExpiration(), ChronoUnit.MILLIS))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    @Transactional
    public RefreshToken rotate(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Refresh token inválido ou não encontrado."));

        if (existing.isRevoked()) {
            User compromisedUser = existing.getUser();
            refreshTokenRepository.deleteByUser(compromisedUser);
            throw new BadCredentialsException(
                    "Reutilização de token detectada. Todas as sessões foram encerradas por segurança. "
                            + "Faça login novamente.");
        }

        // ── Expiration check
        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(existing);
            throw new IllegalStateException(
                    "Refresh token expirado. Faça login novamente.");
        }

        // ── Rotation
        existing.setRevoked(true);

        return create(existing.getUser());
    }

    @Override
    @Transactional
    public void revoke(String tokenValue) {
        if (!refreshTokenRepository.findByToken(tokenValue).isPresent()) {
            throw new EntityNotFoundException("Refresh token não encontrado.");
        }
        refreshTokenRepository.deleteByToken(tokenValue);
    }

    @Override
    @Transactional
    public void revokeAllByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}