package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.config.JwtTokenProvider;
import com.andretti101.escolaweb.dto.request.ChangePasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.ForgotPasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.LoginRequestDTO;
import com.andretti101.escolaweb.dto.request.RefreshTokenRequestDTO;
import com.andretti101.escolaweb.dto.request.ResetPasswordRequestDTO;
import com.andretti101.escolaweb.dto.response.AuthResponseDTO;
import com.andretti101.escolaweb.model.entity.PasswordResetToken;
import com.andretti101.escolaweb.model.entity.RefreshToken;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.PasswordResetTokenRepository;
import com.andretti101.escolaweb.repository.RefreshTokenRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.exception.UnauthorizedOperationException;
import com.andretti101.escolaweb.service.impl.AuthServiceImpl;
import com.andretti101.escolaweb.service.impl.AuthenticatedUserServiceImpl;
import com.andretti101.escolaweb.service.impl.CustomUserDetailsService;
import com.andretti101.escolaweb.service.impl.RefreshTokenServiceImpl;
import com.andretti101.escolaweb.service.impl.UserServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthAndRefreshTokenServiceTest {

    @Nested
    @DisplayName("AuthServiceImpl Unit Tests")
    class AuthServiceTests {

        @Mock
        private AuthenticationManager authenticationManager;
        @Mock
        private JwtTokenProvider jwtTokenProvider;
        @Mock
        private CustomUserDetailsService userDetailsService;
        @Mock
        private RefreshTokenService refreshTokenService;
        @Mock
        private UserService userService;
        @Mock
        private PasswordResetTokenRepository passwordResetTokenRepository;
        @Mock
        private EmailService emailService;

        @InjectMocks
        private AuthServiceImpl authService;

        private User principalUser;
        private UserDetails userDetails;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(authService, "passwordResetExpiration", 900_000L);
            principalUser = new Teacher();
            principalUser.setId(1);
            principalUser.setName("Helena Cristina Vieira");
            principalUser.setEmail("direcao@example.com");
            principalUser.setPassword("encodedPass");
            principalUser.setRole(UserRole.PRINCIPAL);
            principalUser.setActive(true);

            userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("direcao@example.com")
                    .password("encodedPass")
                    .authorities(new SimpleGrantedAuthority("ROLE_PRINCIPAL"))
                    .build();
        }

        @Test
        @DisplayName("login autentica credenciais e retorna AuthResponseDTO com access e refresh token")
        void login_validCredentials_returnsTokens() {
            LoginRequestDTO dto = new LoginRequestDTO("direcao@example.com", "senha123");
            RefreshToken refreshToken = RefreshToken.builder()
                    .token("refresh-uuid-123")
                    .user(principalUser)
                    .revoked(false)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            when(userDetailsService.loadUserByUsername("direcao@example.com")).thenReturn(userDetails);
            when(userService.findByEmail("direcao@example.com")).thenReturn(Optional.of(principalUser));
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("jwt-access-token");
            when(refreshTokenService.create(principalUser)).thenReturn(refreshToken);
            when(jwtTokenProvider.getExpiration()).thenReturn(900_000L);
            when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604_800_000L);

            AuthResponseDTO response = authService.login(dto);

            assertNotNull(response);
            assertEquals("jwt-access-token", response.accessToken());
            assertEquals("refresh-uuid-123", response.refreshToken());
            assertEquals("Bearer", response.tokenType());
            assertEquals("direcao@example.com", response.email());
            assertEquals("ROLE_PRINCIPAL", response.role());
            assertEquals(1, response.userId());
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("refresh rotaciona o refresh token e gera novo access token")
        void refresh_validToken_rotatesAndReturnsNewTokens() {
            RefreshToken rotated = RefreshToken.builder()
                    .token("new-refresh-uuid")
                    .user(principalUser)
                    .revoked(false)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            when(refreshTokenService.rotate("old-refresh-uuid")).thenReturn(rotated);
            when(userDetailsService.loadUserByUsername("direcao@example.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("new-jwt-token");
            when(jwtTokenProvider.getExpiration()).thenReturn(900_000L);
            when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604_800_000L);

            AuthResponseDTO response = authService.refresh(new RefreshTokenRequestDTO("old-refresh-uuid"));

            assertEquals("new-jwt-token", response.accessToken());
            assertEquals("new-refresh-uuid", response.refreshToken());
            verify(refreshTokenService).rotate("old-refresh-uuid");
        }

        @Test
        @DisplayName("logout revoga o refresh token informado")
        void logout_delegatesToRefreshTokenService() {
            authService.logout(new RefreshTokenRequestDTO("token-to-revoke"));
            verify(refreshTokenService).revoke("token-to-revoke");
        }

        @Test
        @DisplayName("changePassword altera a senha e revoga todos os refresh tokens do usuário")
        void changePassword_validUser_updatesPasswordAndRevokesTokens() {
            when(userService.findByEmail("direcao@example.com")).thenReturn(Optional.of(principalUser));

            ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("senha123", "novaSenha456");
            authService.changePassword("direcao@example.com", dto);

            verify(userService).changePassword(1, "senha123", "novaSenha456");
            verify(refreshTokenService).revokeAllByUser(principalUser);
        }

        @Test
        @DisplayName("forgotPassword ignora silenciosamente e-mail inexistente sem enviar e-mail")
        void forgotPassword_nonExistentEmail_doesNothing() {
            when(userService.findByEmail("inexistente@example.com")).thenReturn(Optional.empty());

            authService.forgotPassword(new ForgotPasswordRequestDTO("inexistente@example.com"));

            verifyNoInteractions(passwordResetTokenRepository, emailService);
        }

        @Test
        @DisplayName("forgotPassword remove tokens antigos, salva novo token e envia e-mail")
        void forgotPassword_existingEmail_createsTokenAndSendsEmail() {
            when(userService.findByEmail("direcao@example.com")).thenReturn(Optional.of(principalUser));

            authService.forgotPassword(new ForgotPasswordRequestDTO("direcao@example.com"));

            verify(passwordResetTokenRepository).deleteByUser(principalUser);
            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(passwordResetTokenRepository).save(captor.capture());
            PasswordResetToken savedToken = captor.getValue();
            assertNotNull(savedToken.getToken());
            assertEquals(principalUser, savedToken.getUser());
            assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
            verify(emailService).sendPasswordResetEmail(eq("direcao@example.com"), eq("Helena Cristina Vieira"), eq(savedToken.getToken()));
        }

        @Test
        @DisplayName("resetPassword lança IllegalArgumentException quando token não existe")
        void resetPassword_invalidToken_throwsIllegalArgumentException() {
            when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    authService.resetPassword(new ResetPasswordRequestDTO("bad-token", "novaSenha123")));
        }

        @Test
        @DisplayName("resetPassword exclui token expirado e lança IllegalArgumentException")
        void resetPassword_expiredToken_deletesTokenAndThrows() {
            PasswordResetToken expired = PasswordResetToken.builder()
                    .token("expired-token")
                    .user(principalUser)
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

            assertThrows(IllegalArgumentException.class, () ->
                    authService.resetPassword(new ResetPasswordRequestDTO("expired-token", "novaSenha123")));

            verify(passwordResetTokenRepository).delete(expired);
            verifyNoInteractions(userService, refreshTokenService);
        }

        @Test
        @DisplayName("resetPassword redefine a senha, revoga sessões e limpa tokens")
        void resetPassword_validToken_resetsPasswordAndCleansUp() {
            PasswordResetToken valid = PasswordResetToken.builder()
                    .token("valid-token")
                    .user(principalUser)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(valid));

            authService.resetPassword(new ResetPasswordRequestDTO("valid-token", "novaSenha123"));

            verify(userService).resetPassword(1, "novaSenha123");
            verify(refreshTokenService).revokeAllByUser(principalUser);
            verify(passwordResetTokenRepository).delete(valid);
            verify(passwordResetTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("RefreshTokenServiceImpl Unit Tests")
    class RefreshTokenServiceTests {

        @Mock
        private RefreshTokenRepository refreshTokenRepository;
        @Mock
        private JwtTokenProvider jwtTokenProvider;

        @InjectMocks
        private RefreshTokenServiceImpl refreshTokenService;

        private User user;

        @BeforeEach
        void setUp() {
            user = new Teacher();
            user.setId(2);
            user.setEmail("secretaria@example.com");
            user.setName("Ricardo");
            user.setRole(UserRole.SECRETARY);
            user.setActive(true);
        }

        @Test
        @DisplayName("create gera novo RefreshToken não revogado com expiração configurada")
        void create_savesValidRefreshToken() {
            when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604_800_000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken created = refreshTokenService.create(user);

            assertNotNull(created.getToken());
            assertFalse(created.isRevoked());
            assertEquals(user, created.getUser());
            assertTrue(created.getExpiresAt().isAfter(LocalDateTime.now()));
        }

        @Test
        @DisplayName("rotate marca token atual como revogado e cria novo token")
        void rotate_validToken_marksRevokedAndCreatesNew() {
            RefreshToken existing = RefreshToken.builder()
                    .token("valid-rt")
                    .user(user)
                    .revoked(false)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();
            when(refreshTokenRepository.findByToken("valid-rt")).thenReturn(Optional.of(existing));
            when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604_800_000L);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken rotated = refreshTokenService.rotate("valid-rt");

            assertTrue(existing.isRevoked());
            assertNotNull(rotated);
            assertNotEquals("valid-rt", rotated.getToken());
            assertFalse(rotated.isRevoked());
        }

        @Test
        @DisplayName("rotate detecta reutilização de token revogado, apaga todos os tokens do usuário e lança BadCredentialsException")
        void rotate_revokedToken_deletesAllUserTokensAndThrowsBadCredentials() {
            RefreshToken revoked = RefreshToken.builder()
                    .token("reused-rt")
                    .user(user)
                    .revoked(true)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();
            when(refreshTokenRepository.findByToken("reused-rt")).thenReturn(Optional.of(revoked));

            assertThrows(BadCredentialsException.class, () -> refreshTokenService.rotate("reused-rt"));

            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("rotate remove token expirado e lança IllegalStateException")
        void rotate_expiredToken_deletesTokenAndThrowsIllegalState() {
            RefreshToken expired = RefreshToken.builder()
                    .token("expired-rt")
                    .user(user)
                    .revoked(false)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(refreshTokenRepository.findByToken("expired-rt")).thenReturn(Optional.of(expired));

            assertThrows(IllegalStateException.class, () -> refreshTokenService.rotate("expired-rt"));

            verify(refreshTokenRepository).delete(expired);
        }

        @Test
        @DisplayName("revoke lança EntityNotFoundException se token não existir e exclui se existir")
        void revoke_checksExistenceAndDeletes() {
            when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> refreshTokenService.revoke("missing"));

            RefreshToken existing = RefreshToken.builder().token("found").build();
            when(refreshTokenRepository.findByToken("found")).thenReturn(Optional.of(existing));
            refreshTokenService.revoke("found");
            verify(refreshTokenRepository).deleteByToken("found");
        }
    }

    @Nested
    @DisplayName("UserServiceImpl & CustomUserDetailsService & AuthenticatedUserServiceImpl Unit Tests")
    class UserAndSecurityServiceTests {

        @Mock
        private UserRepository userRepository;
        @Mock
        private PasswordEncoder passwordEncoder;

        @InjectMocks
        private UserServiceImpl userService;

        @InjectMocks
        private CustomUserDetailsService customUserDetailsService;

        @InjectMocks
        private AuthenticatedUserServiceImpl authenticatedUserService;

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("UserServiceImpl.changePassword valida senha atual e codifica nova senha")
        void changePassword_checksCurrentPasswordAndEncodesNew() {
            User user = new Teacher();
            user.setId(5);
            user.setPassword("oldHash");
            when(userRepository.findById(5)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongCurrent", "oldHash")).thenReturn(false);

            assertThrows(IllegalArgumentException.class, () ->
                    userService.changePassword(5, "wrongCurrent", "newPass"));

            when(passwordEncoder.matches("rightCurrent", "oldHash")).thenReturn(true);
            when(passwordEncoder.encode("newPass")).thenReturn("newHash");

            userService.changePassword(5, "rightCurrent", "newPass");
            assertEquals("newHash", user.getPassword());
        }

        @Test
        @DisplayName("UserServiceImpl activate/deactivate altera flag active")
        void activateAndDeactivate_togglesActiveFlag() {
            User user = new Teacher();
            user.setId(7);
            user.setActive(false);
            when(userRepository.findById(7)).thenReturn(Optional.of(user));

            userService.activate(7);
            assertTrue(user.isActive());

            userService.deactivate(7);
            assertFalse(user.isActive());
        }

        @Test
        @DisplayName("CustomUserDetailsService constrói UserDetails com ROLE_ e flag disabled correta")
        void loadUserByUsername_returnsUserDetailsOrThrows() {
            User user = new Teacher();
            user.setEmail("prof@example.com");
            user.setPassword("hashed");
            user.setRole(UserRole.TEACHER);
            user.setActive(false);

            when(userRepository.findByEmail("prof@example.com")).thenReturn(Optional.of(user));
            UserDetails details = customUserDetailsService.loadUserByUsername("prof@example.com");

            assertEquals("prof@example.com", details.getUsername());
            assertFalse(details.isEnabled());
            assertTrue(details.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER")));

            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
            assertThrows(UsernameNotFoundException.class, () ->
                    customUserDetailsService.loadUserByUsername("unknown@example.com"));
        }

        @Test
        @DisplayName("AuthenticatedUserServiceImpl enforceTeacherOwnership e enforceStudentOwnership validam propriedade")
        void enforceOwnership_throwsWhenNotOwner() {
            Teacher loggedTeacher = Teacher.builder().id(10).email("t10@example.com").role(UserRole.TEACHER).build();
            Teacher otherTeacher = Teacher.builder().id(20).email("t20@example.com").role(UserRole.TEACHER).build();
            TeacherClassSubject tcs = TeacherClassSubject.builder().id(100).teacher(otherTeacher).build();

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("t10@example.com", null,
                            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
            when(userRepository.findByEmail("t10@example.com")).thenReturn(Optional.of(loggedTeacher));

            assertThrows(UnauthorizedOperationException.class, () ->
                    authenticatedUserService.enforceTeacherOwnership(tcs));

            Student loggedStudent = Student.builder().id(30).email("s30@example.com").role(UserRole.STUDENT).build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("s30@example.com", null,
                            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
            when(userRepository.findByEmail("s30@example.com")).thenReturn(Optional.of(loggedStudent));

            assertDoesNotThrow(() -> authenticatedUserService.enforceStudentOwnership(30));
            assertThrows(UnauthorizedOperationException.class, () ->
                    authenticatedUserService.enforceStudentOwnership(99));
        }
    }
}
