package com.andretti101.escolaweb.controller;

import com.andretti101.escolaweb.dto.request.StudentUpdateRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherUpdateRequestDTO;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.Teacher;
import com.andretti101.escolaweb.repository.StudentRepository;
import com.andretti101.escolaweb.repository.TeacherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentAndTeacherUpdateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/teachers/{id} without password should update teacher and keep existing password")
    void updateTeacher_withoutPassword_preservesExistingPassword() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Professor Original")
                .email("prof.original@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);
        String initialPasswordHash = teacher.getPassword();

        TeacherUpdateRequestDTO updateDto = new TeacherUpdateRequestDTO(
                "Professor Atualizado",
                "prof.atualizado@escola.com",
                null
        );

        mockMvc.perform(put("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Professor Atualizado"))
                .andExpect(jsonPath("$.email").value("prof.atualizado@escola.com"));

        Teacher reloaded = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertEquals("Professor Atualizado", reloaded.getName());
        assertEquals("prof.atualizado@escola.com", reloaded.getEmail());
        assertEquals(initialPasswordHash, reloaded.getPassword(), "Existing password hash must remain untouched");
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with empty password string should normalize to null and keep existing password")
    void patchTeacher_withEmptyPassword_preservesExistingPassword() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Professor Patch")
                .email("prof.patch@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);
        String initialPasswordHash = teacher.getPassword();

        String jsonPayload = """
                {
                    "name": "Professor Patch Renomeado",
                    "email": "prof.patch@escola.com",
                    "password": ""
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Professor Patch Renomeado"));

        Teacher reloaded = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertEquals("Professor Patch Renomeado", reloaded.getName());
        assertEquals(initialPasswordHash, reloaded.getPassword());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/teachers/{id} with valid new password should update password")
    void updateTeacher_withValidNewPassword_updatesPassword() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Professor Com Nova Senha")
                .email("prof.novasenha@escola.com")
                .password(passwordEncoder.encode("senhaAntiga123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);

        TeacherUpdateRequestDTO updateDto = new TeacherUpdateRequestDTO(
                "Professor Com Nova Senha",
                "prof.novasenha@escola.com",
                "novaSenhaForte456"
        );

        mockMvc.perform(put("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        Teacher reloaded = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("novaSenhaForte456", reloaded.getPassword()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/teachers/{id} with short password should fail validation with 400")
    void updateTeacher_withShortPassword_failsValidation() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Professor Teste Validacao")
                .email("prof.validacao@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);

        String jsonPayload = """
                {
                    "name": "Professor Teste Validacao",
                    "email": "prof.validacao@escola.com",
                    "password": "curta"
                }
                """;

        mockMvc.perform(put("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/students/{id} without password should update student and keep existing password")
    void updateStudent_withoutPassword_preservesExistingPassword() throws Exception {
        Student student = Student.builder()
                .name("Aluno Original")
                .email("aluno.original@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-ORIG-01")
                .birthDate(LocalDate.of(2008, 1, 15))
                .active(true)
                .build();
        student = studentRepository.save(student);
        String initialPasswordHash = student.getPassword();

        StudentUpdateRequestDTO updateDto = new StudentUpdateRequestDTO(
                "Aluno Atualizado",
                "aluno.atualizado@escola.com",
                null,
                "11911112222",
                "11933334444",
                "MAT-ORIG-01",
                LocalDate.of(2008, 1, 15)
        );

        mockMvc.perform(put("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aluno Atualizado"))
                .andExpect(jsonPath("$.email").value("aluno.atualizado@escola.com"));

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertEquals("Aluno Atualizado", reloaded.getName());
        assertEquals("aluno.atualizado@escola.com", reloaded.getEmail());
        assertEquals("11911112222", reloaded.getOwnPhone());
        assertEquals(initialPasswordHash, reloaded.getPassword(), "Existing password hash must remain untouched");
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with empty password string should normalize to null and keep existing password")
    void patchStudent_withEmptyPassword_preservesExistingPassword() throws Exception {
        Student student = Student.builder()
                .name("Aluno Patch")
                .email("aluno.patch@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-PATCH-01")
                .birthDate(LocalDate.of(2009, 2, 20))
                .active(true)
                .build();
        student = studentRepository.save(student);
        String initialPasswordHash = student.getPassword();

        String jsonPayload = """
                {
                    "name": "Aluno Patch Renomeado",
                    "email": "aluno.patch@escola.com",
                    "password": "   ",
                    "registrationNumber": "MAT-PATCH-01",
                    "birthDate": "2009-02-20"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aluno Patch Renomeado"));

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertEquals("Aluno Patch Renomeado", reloaded.getName());
        assertEquals(initialPasswordHash, reloaded.getPassword());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/students/{id} with valid new password should update password")
    void updateStudent_withValidNewPassword_updatesPassword() throws Exception {
        Student student = Student.builder()
                .name("Aluno Nova Senha")
                .email("aluno.novasenha@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-SENHA-01")
                .birthDate(LocalDate.of(2009, 3, 10))
                .active(true)
                .build();
        student = studentRepository.save(student);

        StudentUpdateRequestDTO updateDto = new StudentUpdateRequestDTO(
                "Aluno Nova Senha",
                "aluno.novasenha@escola.com",
                "novaSenhaForte999",
                null,
                null,
                "MAT-SENHA-01",
                LocalDate.of(2009, 3, 10)
        );

        mockMvc.perform(put("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("novaSenhaForte999", reloaded.getPassword()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/students/{id} with short password should fail validation with 400")
    void updateStudent_withShortPassword_failsValidation() throws Exception {
        Student student = Student.builder()
                .name("Aluno Senha Curta")
                .email("aluno.senhacurta@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-CURTA-01")
                .birthDate(LocalDate.of(2009, 3, 10))
                .active(true)
                .build();
        student = studentRepository.save(student);

        String jsonPayload = """
                {
                    "name": "Aluno Senha Curta",
                    "email": "aluno.senhacurta@escola.com",
                    "password": "123",
                    "registrationNumber": "MAT-CURTA-01",
                    "birthDate": "2009-03-10"
                }
                """;

        mockMvc.perform(put("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/teachers/{id} with omitted password property preserves existing password")
    void updateTeacher_withOmittedPasswordKey_preservesExistingPassword() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Professor Sem Campo")
                .email("prof.semcampo@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);
        String initialPasswordHash = teacher.getPassword();

        String jsonPayload = """
                {
                    "name": "Professor Sem Campo Atualizado",
                    "email": "prof.semcampo2@escola.com"
                }
                """;

        mockMvc.perform(put("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Professor Sem Campo Atualizado"));

        Teacher reloaded = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertEquals(initialPasswordHash, reloaded.getPassword());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with valid new password updates password")
    void patchTeacher_withValidNewPassword_updatesPassword() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Professor Patch Nova Senha")
                .email("prof.patchsenha@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);

        String jsonPayload = """
                {
                    "name": "Professor Patch Nova Senha",
                    "email": "prof.patchsenha@escola.com",
                    "password": "novaSenhaValida456"
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        Teacher reloaded = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("novaSenhaValida456", reloaded.getPassword()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with short password fails with 400")
    void patchTeacher_withShortPassword_failsValidation() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Professor Patch Curta")
                .email("prof.patchcurta@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);

        String jsonPayload = """
                {
                    "name": "Professor Patch Curta",
                    "email": "prof.patchcurta@escola.com",
                    "password": "123"
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/students/{id} with omitted password property preserves existing password")
    void updateStudent_withOmittedPasswordKey_preservesExistingPassword() throws Exception {
        Student student = Student.builder()
                .name("Aluno Sem Campo")
                .email("aluno.semcampo@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-OMIT-01")
                .birthDate(LocalDate.of(2009, 5, 20))
                .active(true)
                .build();
        student = studentRepository.save(student);
        String initialPasswordHash = student.getPassword();

        String jsonPayload = """
                {
                    "name": "Aluno Sem Campo Atualizado",
                    "email": "aluno.semcampo2@escola.com",
                    "registrationNumber": "MAT-OMIT-01",
                    "birthDate": "2009-05-20"
                }
                """;

        mockMvc.perform(put("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aluno Sem Campo Atualizado"));

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertEquals(initialPasswordHash, reloaded.getPassword());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with valid new password updates password")
    void patchStudent_withValidNewPassword_updatesPassword() throws Exception {
        Student student = Student.builder()
                .name("Aluno Patch Nova Senha")
                .email("aluno.patchsenha@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-PTSENHA-01")
                .birthDate(LocalDate.of(2009, 4, 12))
                .active(true)
                .build();
        student = studentRepository.save(student);

        String jsonPayload = """
                {
                    "name": "Aluno Patch Nova Senha",
                    "email": "aluno.patchsenha@escola.com",
                    "password": "novaSenhaValida789",
                    "registrationNumber": "MAT-PTSENHA-01",
                    "birthDate": "2009-04-12"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk());

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("novaSenhaValida789", reloaded.getPassword()));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with short password fails with 400")
    void patchStudent_withShortPassword_failsValidation() throws Exception {
        Student student = Student.builder()
                .name("Aluno Patch Curta")
                .email("aluno.patchcurta@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-PTCURTA-01")
                .birthDate(LocalDate.of(2009, 4, 12))
                .active(true)
                .build();
        student = studentRepository.save(student);

        String jsonPayload = """
                {
                    "name": "Aluno Patch Curta",
                    "email": "aluno.patchcurta@escola.com",
                    "password": "123",
                    "registrationNumber": "MAT-PTCURTA-01",
                    "birthDate": "2009-04-12"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("PUT /api/teachers/{id} by non-secretary role returns 403 Forbidden")
    void updateTeacher_forbiddenForNonSecretary_returns403() throws Exception {
        TeacherUpdateRequestDTO updateDto = new TeacherUpdateRequestDTO(
                "Professor Nao Permitido",
                "prof.naopermitido@escola.com",
                null
        );

        mockMvc.perform(put("/api/teachers/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("PUT /api/students/{id} by non-secretary role returns 403 Forbidden")
    void updateStudent_forbiddenForNonSecretary_returns403() throws Exception {
        StudentUpdateRequestDTO updateDto = new StudentUpdateRequestDTO(
                "Aluno Nao Permitido",
                "aluno.naopermitido@escola.com",
                null,
                null,
                null,
                "MAT-FORBID-01",
                LocalDate.of(2008, 1, 1)
        );

        mockMvc.perform(put("/api/students/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/teachers/{id} with existing email of another teacher returns 422")
    void updateTeacher_duplicateEmail_returns422() throws Exception {
        Teacher teacher1 = Teacher.builder()
                .name("Professor 1")
                .email("prof1.dup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .active(true)
                .build();
        teacher1 = teacherRepository.save(teacher1);

        Teacher teacher2 = Teacher.builder()
                .name("Professor 2")
                .email("prof2.dup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .active(true)
                .build();
        teacher2 = teacherRepository.save(teacher2);

        TeacherUpdateRequestDTO updateDto = new TeacherUpdateRequestDTO(
                "Professor 2 Renomeado",
                "prof1.dup@escola.com",
                null
        );

        mockMvc.perform(put("/api/teachers/{id}", teacher2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PUT /api/students/{id} with existing email of another student returns 422")
    void updateStudent_duplicateEmail_returns422() throws Exception {
        Student student1 = Student.builder()
                .name("Aluno 1")
                .email("aluno1.dup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-DUP-01")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build();
        student1 = studentRepository.save(student1);

        Student student2 = Student.builder()
                .name("Aluno 2")
                .email("aluno2.dup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-DUP-02")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build();
        student2 = studentRepository.save(student2);

        StudentUpdateRequestDTO updateDto = new StudentUpdateRequestDTO(
                "Aluno 2 Renomeado",
                "aluno1.dup@escola.com",
                null,
                null,
                null,
                "MAT-DUP-02",
                LocalDate.of(2008, 1, 1)
        );

        mockMvc.perform(put("/api/students/{id}", student2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnprocessableEntity());
    }
}
