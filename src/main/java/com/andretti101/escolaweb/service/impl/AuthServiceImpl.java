package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.config.JwtTokenProvider;
import com.andretti101.escolaweb.dto.request.ChangePasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.LoginRequestDTO;
import com.andretti101.escolaweb.dto.request.RefreshTokenRequestDTO;
import com.andretti101.escolaweb.dto.response.AuthResponseDTO;
import com.andretti101.escolaweb.model.entity.RefreshToken;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.service.AuthService;
import com.andretti101.escolaweb.service.RefreshTokenService;
import com.andretti101.escolaweb.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

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

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequestDTO dto) {
        User user = findUserByEmailOrThrow(email);

        userService.changePassword(user.getId(), dto.currentPassword(), dto.newPassword());

        refreshTokenService.revokeAllByUser(user);
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
