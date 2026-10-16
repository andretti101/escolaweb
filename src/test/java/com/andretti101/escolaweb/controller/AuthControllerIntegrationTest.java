package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.ChangePasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.ForgotPasswordRequestDTO;
import com.andretti101.escolaweb.dto.request.LoginRequestDTO;
import com.andretti101.escolaweb.dto.request.RefreshTokenRequestDTO;
import com.andretti101.escolaweb.dto.request.ResetPasswordRequestDTO;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.PasswordResetTokenRepository;
import com.andretti101.escolaweb.repository.RefreshTokenRepository;
import com.andretti101.escolaweb.repository.StudentRepository;
import com.andretti101.escolaweb.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    private Student testUser;

    @BeforeEach
    void setUp() {
        testUser = studentRepository.save(Student.builder()
                .name("Aluno Auth Integration")
                .email("aluno.auth.integration@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.STUDENT)
                .registrationNumber("2026AUTH01")
                .birthDate(LocalDate.of(2010, 1, 15))
                .guardianPhone("(48) 99999-1111")
                .active(true)
                .build());
    }

    @Test
    @DisplayName("POST /auth/login retorna 200 com JWT e RefreshToken para credenciais válidas e 401 para senha inválida ou usuário inativo")
    void login_validInvalidAndInactiveUser() throws Exception {
        // 1. Login válido
        LoginRequestDTO validLogin = new LoginRequestDTO(testUser.getEmail(), "senha123");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));

        // 2. Senha incorreta -> 401
        LoginRequestDTO wrongPass = new LoginRequestDTO(testUser.getEmail(), "senhaErrada");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized());

        // 3. Usuário desativado -> 401
        testUser.setActive(false);
        studentRepository.saveAndFlush(testUser);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/refresh rotaciona o refresh token e bloqueia reutilização do token anterior com 401")
    void refresh_rotatesTokenAndDetectsReuse() throws Exception {
        MvcResult loginRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(testUser.getEmail(), "senha123"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        String firstRefreshToken = loginJson.get("refreshToken").asText();

        // Rotaciona o primeiro refresh token -> sucesso
        MvcResult refreshRes = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDTO(firstRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshRes.getResponse().getContentAsString());
        String secondRefreshToken = refreshJson.get("refreshToken").asText();
        assertNotEquals(firstRefreshToken, secondRefreshToken);

        // Tentativa de reutilizar o primeiro refresh token já revogado -> 401 e invalida todos os tokens do usuário
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDTO(firstRefreshToken))))
                .andExpect(status().isUnauthorized());

        assertTrue(refreshTokenRepository.findByToken(secondRefreshToken).isEmpty(),
                "O segundo refresh token também deve ter sido removido após detecção de reutilização");
    }

    @Test
    @DisplayName("POST /auth/logout revoga o refresh token e PATCH /auth/change-password altera a senha com JWT válido")
    void changePasswordAndLogout_flowSucceeds() throws Exception {
        MvcResult loginRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(testUser.getEmail(), "senha123"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();

        // Alterar senha autenticado via Bearer token
        ChangePasswordRequestDTO changeDto = new ChangePasswordRequestDTO("senha123", "novaSenhaSegura123");
        mockMvc.perform(patch("/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeDto)))
                .andExpect(status().isNoContent());

        // Login com a nova senha funciona
        MvcResult newLoginRes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(testUser.getEmail(), "novaSenhaSegura123"))))
                .andExpect(status().isOk())
                .andReturn();

        String newRefreshToken = objectMapper.readTree(newLoginRes.getResponse().getContentAsString())
                .get("refreshToken").asText();

        // Logout revoga o refresh token
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequestDTO(newRefreshToken))))
                .andExpect(status().isNoContent());

        assertTrue(refreshTokenRepository.findByToken(newRefreshToken).isEmpty());
    }

    @Test
    @DisplayName("POST /auth/forgot-password e POST /auth/reset-password redefinem a senha usando o token enviado por e-mail")
    void forgotAndResetPassword_endToEndFlow() throws Exception {
        ForgotPasswordRequestDTO forgotDto = new ForgotPasswordRequestDTO(testUser.getEmail());

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(testUser.getEmail()), eq(testUser.getName()), tokenCaptor.capture());
        String generatedToken = tokenCaptor.getValue();
        assertNotNull(generatedToken);
        assertTrue(passwordResetTokenRepository.findByToken(generatedToken).isPresent());

        ResetPasswordRequestDTO resetDto = new ResetPasswordRequestDTO(generatedToken, "senhaRedefinida999");
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isNoContent());

        assertTrue(passwordResetTokenRepository.findByToken(generatedToken).isEmpty());

        // Login com a senha redefinida deve funcionar
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO(testUser.getEmail(), "senhaRedefinida999"))))
                .andExpect(status().isOk());
    }
}
