package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.config.JwtTokenProvider;
import com.andretti101.escolaweb.dto.request.LoginRequestDTO;
import com.andretti101.escolaweb.dto.request.RefreshTokenRequestDTO;
import com.andretti101.escolaweb.dto.response.AuthResponseDTO;
import com.andretti101.escolaweb.model.entity.RefreshToken;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.RefreshTokenService;
import com.andretti101.escolaweb.service.impl.CustomUserDetailsService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.email());
        User user = findUserOrThrow(dto.email());
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.create(user);

        return ResponseEntity.ok(buildResponse(accessToken, refreshToken.getToken(), userDetails));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        RefreshToken newRefreshToken = refreshTokenService.rotate(dto.refreshToken());

        UserDetails userDetails = userDetailsService
                .loadUserByUsername(newRefreshToken.getUser().getEmail());

        String newAccessToken = jwtTokenProvider.generateToken(userDetails);

        return ResponseEntity.ok(
                buildResponse(newAccessToken, newRefreshToken.getToken(), userDetails));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO dto) {
        refreshTokenService.revoke(dto.refreshToken());
        return ResponseEntity.noContent().build();
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
                jwtTokenProvider.getRefreshExpiration()
        );
    }

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado: " + email));
    }
}
