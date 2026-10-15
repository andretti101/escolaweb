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
    private com.andretti101.escolaweb.repository.TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private com.andretti101.escolaweb.repository.ClassRoomRepository classRoomRepository;

    @Autowired
    private com.andretti101.escolaweb.repository.AcademicYearRepository academicYearRepository;

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

    @Autowired
    private com.andretti101.escolaweb.repository.SubjectRepository subjectRepository;

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("POST /api/teachers and PUT /api/teachers/{id} support zero, one, or multiple subjects")
    void createAndUpdateTeacher_withZeroOneOrMultipleSubjects_succeeds() throws Exception {
        com.andretti101.escolaweb.model.entity.Subject subjA = subjectRepository.save(
                com.andretti101.escolaweb.model.entity.Subject.builder()
                        .name("Disciplina Teste A")
                        .description("Desc A")
                        .active(true)
                        .build());
        com.andretti101.escolaweb.model.entity.Subject subjB = subjectRepository.save(
                com.andretti101.escolaweb.model.entity.Subject.builder()
                        .name("Disciplina Teste B")
                        .description("Desc B")
                        .active(true)
                        .build());

        // 1. Create teacher without subjects (optional)
        com.andretti101.escolaweb.dto.request.TeacherRequestDTO createNoSubjects =
                new com.andretti101.escolaweb.dto.request.TeacherRequestDTO(
                        "Prof Sem Disciplinas",
                        "prof.semdisc@escola.com",
                        "senha12345",
                        null
                );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createNoSubjects)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjects").isArray())
                .andExpect(jsonPath("$.subjects.length()").value(0));

        // 2. Create teacher with 2 subjects
        com.andretti101.escolaweb.dto.request.TeacherRequestDTO createWithSubjects =
                new com.andretti101.escolaweb.dto.request.TeacherRequestDTO(
                        "Prof Com Disciplinas",
                        "prof.comdisc@escola.com",
                        "senha12345",
                        java.util.List.of(subjA.getId(), subjB.getId())
                );

        String responseJson = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createWithSubjects)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subjects.length()").value(2))
                .andExpect(jsonPath("$.subjects[0].name").value("Disciplina Teste A"))
                .andExpect(jsonPath("$.subjects[1].name").value("Disciplina Teste B"))
                .andReturn().getResponse().getContentAsString();

        Integer teacherId = objectMapper.readTree(responseJson).get("id").asInt();

        // 3. Update teacher omitting subjectIds (null) preserves existing subjects
        TeacherUpdateRequestDTO updateOmitSubjects = new TeacherUpdateRequestDTO(
                "Prof Com Disciplinas Renomeado",
                "prof.comdisc@escola.com",
                null,
                null
        );

        mockMvc.perform(put("/api/teachers/{id}", teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOmitSubjects)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prof Com Disciplinas Renomeado"))
                .andExpect(jsonPath("$.subjects.length()").value(2));

        // 4. Update teacher replacing subjects with 1 subject
        TeacherUpdateRequestDTO updateOneSubject = new TeacherUpdateRequestDTO(
                "Prof Com Disciplinas Renomeado",
                "prof.comdisc@escola.com",
                null,
                java.util.List.of(subjB.getId())
        );

        mockMvc.perform(put("/api/teachers/{id}", teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOneSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(1))
                .andExpect(jsonPath("$.subjects[0].id").value(subjB.getId()));

        // 5. Update teacher clearing subjects with empty list []
        TeacherUpdateRequestDTO updateClearSubjects = new TeacherUpdateRequestDTO(
                "Prof Com Disciplinas Renomeado",
                "prof.comdisc@escola.com",
                null,
                java.util.List.of()
        );

        mockMvc.perform(put("/api/teachers/{id}", teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateClearSubjects)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subjects.length()").value(0));

        // 6. Update teacher with non-existent subjectId returns 404
        TeacherUpdateRequestDTO updateInvalidSubject = new TeacherUpdateRequestDTO(
                "Prof Com Disciplinas Renomeado",
                "prof.comdisc@escola.com",
                null,
                java.util.List.of(999999)
        );

        mockMvc.perform(put("/api/teachers/{id}", teacherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateInvalidSubject)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with only name updates name and preserves other fields")
    void patchTeacher_withOnlyName_preservesOtherFields() throws Exception {
        Teacher teacher = Teacher.builder()
                .name("Prof Somente Nome")
                .email("prof.somente.nome@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .build();
        teacher = teacherRepository.save(teacher);
        String initialPassword = teacher.getPassword();

        String payload = """
                {
                    "name": "Prof Nome Alterado Via Patch"
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Prof Nome Alterado Via Patch"))
                .andExpect(jsonPath("$.email").value("prof.somente.nome@escola.com"));

        Teacher reloaded = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertEquals("Prof Nome Alterado Via Patch", reloaded.getName());
        assertEquals("prof.somente.nome@escola.com", reloaded.getEmail());
        assertEquals(initialPassword, reloaded.getPassword());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with only name updates name and preserves other fields")
    void patchStudent_withOnlyName_preservesOtherFields() throws Exception {
        Student student = Student.builder()
                .name("Aluno Somente Nome")
                .email("aluno.somente.nome@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-NAME-ONLY")
                .birthDate(LocalDate.of(2008, 5, 20))
                .active(true)
                .build();
        student = studentRepository.save(student);
        String initialPassword = student.getPassword();

        String payload = """
                {
                    "name": "Aluno Nome Alterado Via Patch"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aluno Nome Alterado Via Patch"))
                .andExpect(jsonPath("$.email").value("aluno.somente.nome@escola.com"))
                .andExpect(jsonPath("$.registrationNumber").value("MAT-NAME-ONLY"));

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertEquals("Aluno Nome Alterado Via Patch", reloaded.getName());
        assertEquals("aluno.somente.nome@escola.com", reloaded.getEmail());
        assertEquals("MAT-NAME-ONLY", reloaded.getRegistrationNumber());
        assertEquals(LocalDate.of(2008, 5, 20), reloaded.getBirthDate());
        assertEquals(initialPassword, reloaded.getPassword());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with only birthDate updates birthDate and preserves other fields")
    void patchStudent_withOnlyBirthDate_preservesOtherFields() throws Exception {
        Student student = Student.builder()
                .name("Aluno Data Nasc")
                .email("aluno.datanasc@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-DATE-ONLY")
                .birthDate(LocalDate.of(2008, 1, 10))
                .active(true)
                .build();
        student = studentRepository.save(student);

        String payload = """
                {
                    "birthDate": "2009-11-25"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aluno Data Nasc"))
                .andExpect(jsonPath("$.birthDate").value("2009-11-25"));

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertEquals("Aluno Data Nasc", reloaded.getName());
        assertEquals(LocalDate.of(2009, 11, 25), reloaded.getBirthDate());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with duplicate email returns 422")
    void patchTeacher_withDuplicateEmail_returns422() throws Exception {
        Teacher teacher1 = teacherRepository.save(Teacher.builder()
                .name("Professor Um")
                .email("prof1.patchdup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .active(true)
                .build());

        Teacher teacher2 = teacherRepository.save(Teacher.builder()
                .name("Professor Dois")
                .email("prof2.patchdup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .active(true)
                .build());

        String payload = """
                {
                    "email": "prof1.patchdup@escola.com"
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with duplicate email returns 422")
    void patchStudent_withDuplicateEmail_returns422() throws Exception {
        Student student1 = studentRepository.save(Student.builder()
                .name("Aluno Um")
                .email("aluno1.patchdup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-P1")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        Student student2 = studentRepository.save(Student.builder()
                .name("Aluno Dois")
                .email("aluno2.patchdup@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-P2")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        String payload = """
                {
                    "email": "aluno1.patchdup@escola.com"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with invalid email format returns 400")
    void patchStudent_withInvalidEmailFormat_returns400() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .name("Aluno Email Invalido")
                .email("aluno.inv@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-INV-EMAIL")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        String payload = """
                {
                    "email": "formato-invalido"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with future birth date returns 400")
    void patchStudent_withFutureBirthDate_returns400() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .name("Aluno Data Futura")
                .email("aluno.futuro@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-FUT-DATE")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        String payload = """
                {
                    "birthDate": "2099-01-01"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with only subjectIds updates subjects and preserves other fields")
    void patchTeacher_withSubjectIds_updatesSubjectsAndPreservesOtherFields() throws Exception {
        com.andretti101.escolaweb.model.entity.Subject subjA = subjectRepository.save(
                com.andretti101.escolaweb.model.entity.Subject.builder()
                        .name("Materia Patch A")
                        .description("Desc A")
                        .active(true)
                        .build());
        com.andretti101.escolaweb.model.entity.Subject subjB = subjectRepository.save(
                com.andretti101.escolaweb.model.entity.Subject.builder()
                        .name("Materia Patch B")
                        .description("Desc B")
                        .active(true)
                        .build());

        Teacher teacher = teacherRepository.save(Teacher.builder()
                .name("Professor Patch Materias")
                .email("prof.patchmat@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .subjects(new java.util.LinkedHashSet<>(java.util.List.of(subjA)))
                .build());

        String payload = String.format("""
                {
                    "subjectIds": [%d]
                }
                """, subjB.getId());

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Professor Patch Materias"))
                .andExpect(jsonPath("$.email").value("prof.patchmat@escola.com"))
                .andExpect(jsonPath("$.subjects.length()").value(1))
                .andExpect(jsonPath("$.subjects[0].id").value(subjB.getId()));

        Teacher reloaded = teacherRepository.findById(teacher.getId()).orElseThrow();
        assertEquals("Professor Patch Materias", reloaded.getName());
        assertEquals("prof.patchmat@escola.com", reloaded.getEmail());
        assertEquals(1, reloaded.getSubjects().size());
        assertEquals(subjB.getId(), reloaded.getSubjects().iterator().next().getId());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} removing active teaching subject returns 422")
    void patchTeacher_removingActiveTeachingSubject_returns422() throws Exception {
        com.andretti101.escolaweb.model.entity.AcademicYear year = academicYearRepository.save(
                com.andretti101.escolaweb.model.entity.AcademicYear.builder()
                        .year(2095)
                        .startDate(LocalDate.of(2095, 2, 1))
                        .endDate(LocalDate.of(2095, 12, 15))
                        .minimumGrade(java.math.BigDecimal.valueOf(6.0))
                        .minimumAttendance(java.math.BigDecimal.valueOf(75.0))
                        .active(true)
                        .build());

        com.andretti101.escolaweb.model.entity.ClassRoom cr = classRoomRepository.save(
                com.andretti101.escolaweb.model.entity.ClassRoom.builder()
                        .name("Turma Ativa 95")
                        .shift(com.andretti101.escolaweb.model.enums.Shift.MORNING)
                        .schoolGrade(com.andretti101.escolaweb.model.enums.SchoolGrade.FUNDAMENTAL_8)
                        .academicYear(year)
                        .periodType(com.andretti101.escolaweb.model.enums.AcademicPeriodType.TRIMESTER)
                        .active(true)
                        .build());

        com.andretti101.escolaweb.model.entity.Subject subj = subjectRepository.save(
                com.andretti101.escolaweb.model.entity.Subject.builder()
                        .name("Fisica Patch")
                        .description("Desc")
                        .active(true)
                        .build());

        Teacher teacher = teacherRepository.save(Teacher.builder()
                .name("Professor Com Alocacao Ativa")
                .email("prof.alocado@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .active(true)
                .subjects(new java.util.LinkedHashSet<>(java.util.List.of(subj)))
                .build());

        teacherClassSubjectRepository.save(
                com.andretti101.escolaweb.model.entity.TeacherClassSubject.builder()
                        .classRoom(cr)
                        .subject(subj)
                        .teacher(teacher)
                        .minAssessmentsPerPeriod(1)
                        .maxAssessmentsPerPeriod(3)
                        .active(true)
                        .build());

        String payload = """
                {
                    "subjectIds": []
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with duplicate registrationNumber returns 422")
    void patchStudent_withDuplicateRegistrationNumber_returns422() throws Exception {
        Student student1 = studentRepository.save(Student.builder()
                .name("Aluno Reg 1")
                .email("aluno.reg1@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-REG-01")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        Student student2 = studentRepository.save(Student.builder()
                .name("Aluno Reg 2")
                .email("aluno.reg2@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-REG-02")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        String payload = """
                {
                    "registrationNumber": "MAT-REG-01"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with only phones updates phones and preserves other fields")
    void patchStudent_withOnlyPhones_preservesOtherFields() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .name("Aluno Telefones")
                .email("aluno.telefones@escola.com")
                .password(passwordEncoder.encode("senhaOriginal123"))
                .registrationNumber("MAT-TEL-01")
                .birthDate(LocalDate.of(2008, 1, 1))
                .ownPhone("11911110000")
                .guardianPhone("11922220000")
                .active(true)
                .build());

        String payload = """
                {
                    "ownPhone": "11999998888",
                    "guardianPhone": "11977776666"
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aluno Telefones"))
                .andExpect(jsonPath("$.email").value("aluno.telefones@escola.com"))
                .andExpect(jsonPath("$.registrationNumber").value("MAT-TEL-01"))
                .andExpect(jsonPath("$.ownPhone").value("11999998888"))
                .andExpect(jsonPath("$.guardianPhone").value("11977776666"));

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        assertEquals("11999998888", reloaded.getOwnPhone());
        assertEquals("11977776666", reloaded.getGuardianPhone());
        assertEquals("Aluno Telefones", reloaded.getName());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with blank name returns 400")
    void patchTeacher_withBlankName_returns400() throws Exception {
        Teacher teacher = teacherRepository.save(Teacher.builder()
                .name("Professor Nome Valido")
                .email("prof.nomeval@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .active(true)
                .build());

        String payload = """
                {
                    "name": "   "
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with blank name returns 400")
    void patchStudent_withBlankName_returns400() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .name("Aluno Nome Valido")
                .email("aluno.nomeval@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-NOM-VAL")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        String payload = """
                {
                    "name": "   "
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/teachers/{id} with blank email returns 400")
    void patchTeacher_withBlankEmail_returns400() throws Exception {
        Teacher teacher = teacherRepository.save(Teacher.builder()
                .name("Professor Email Valido")
                .email("prof.emailval@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .active(true)
                .build());

        String payload = """
                {
                    "email": "   "
                }
                """;

        mockMvc.perform(patch("/api/teachers/{id}", teacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with blank email returns 400")
    void patchStudent_withBlankEmail_returns400() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .name("Aluno Email Valido")
                .email("aluno.emailval@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-EM-VAL")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        String payload = """
                {
                    "email": "   "
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("PATCH /api/students/{id} with blank registrationNumber returns 400")
    void patchStudent_withBlankRegistrationNumber_returns400() throws Exception {
        Student student = studentRepository.save(Student.builder()
                .name("Aluno Reg Valido")
                .email("aluno.regval@escola.com")
                .password(passwordEncoder.encode("senha12345"))
                .registrationNumber("MAT-REG-VAL")
                .birthDate(LocalDate.of(2008, 1, 1))
                .active(true)
                .build());

        String payload = """
                {
                    "registrationNumber": "   "
                }
                """;

        mockMvc.perform(patch("/api/students/{id}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
