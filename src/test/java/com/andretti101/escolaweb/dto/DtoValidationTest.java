package com.andretti101.escolaweb.dto;

import com.andretti101.escolaweb.dto.request.StudentUpdateRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherUpdateRequestDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("TeacherUpdateRequestDTO: valid when password is null")
    void teacherUpdateRequest_validWhenPasswordNull() {
        TeacherUpdateRequestDTO dto = new TeacherUpdateRequestDTO(
                "Professor Girafales",
                "girafales@escola.com",
                null
        );

        Set<ConstraintViolation<TeacherUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is null");
        assertNull(dto.password());
    }

    @Test
    @DisplayName("TeacherUpdateRequestDTO: valid when password is empty string (normalized to null)")
    void teacherUpdateRequest_validWhenPasswordEmpty() {
        TeacherUpdateRequestDTO dto = new TeacherUpdateRequestDTO(
                "Professor Girafales",
                "girafales@escola.com",
                ""
        );

        Set<ConstraintViolation<TeacherUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is empty");
        assertNull(dto.password(), "Password should be normalized to null");
    }

    @Test
    @DisplayName("TeacherUpdateRequestDTO: valid when password is blank whitespace (normalized to null)")
    void teacherUpdateRequest_validWhenPasswordWhitespace() {
        TeacherUpdateRequestDTO dto = new TeacherUpdateRequestDTO(
                "Professor Girafales",
                "girafales@escola.com",
                "   "
        );

        Set<ConstraintViolation<TeacherUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is whitespace");
        assertNull(dto.password(), "Password should be normalized to null");
    }

    @Test
    @DisplayName("TeacherUpdateRequestDTO: valid when password is 8 or more characters")
    void teacherUpdateRequest_validWhenPasswordValid() {
        TeacherUpdateRequestDTO dto = new TeacherUpdateRequestDTO(
                "Professor Girafales",
                "girafales@escola.com",
                "senhaForte123"
        );

        Set<ConstraintViolation<TeacherUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is valid");
        assertEquals("senhaForte123", dto.password());
    }

    @Test
    @DisplayName("TeacherUpdateRequestDTO: invalid when password is shorter than 8 characters")
    void teacherUpdateRequest_invalidWhenPasswordTooShort() {
        TeacherUpdateRequestDTO dto = new TeacherUpdateRequestDTO(
                "Professor Girafales",
                "girafales@escola.com",
                "12345"
        );

        Set<ConstraintViolation<TeacherUpdateRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A senha deve ter entre 8 e 255 caracteres.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("TeacherUpdateRequestDTO: invalid when name or email is blank")
    void teacherUpdateRequest_invalidWhenNameOrEmailBlank() {
        TeacherUpdateRequestDTO dto = new TeacherUpdateRequestDTO(
                "",
                "invalid-email",
                null
        );

        Set<ConstraintViolation<TeacherUpdateRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: valid when password is null")
    void studentUpdateRequest_validWhenPasswordNull() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                null,
                "11999999999",
                "11988888888",
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is null");
        assertNull(dto.password());
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: valid when password is empty string (normalized to null)")
    void studentUpdateRequest_validWhenPasswordEmpty() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                "",
                null,
                null,
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is empty");
        assertNull(dto.password());
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: valid when password is blank whitespace (normalized to null)")
    void studentUpdateRequest_validWhenPasswordWhitespace() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                "    ",
                null,
                null,
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is whitespace");
        assertNull(dto.password());
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: valid when password has 8+ characters")
    void studentUpdateRequest_validWhenPasswordValid() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                "minhaSenhaNova123",
                null,
                null,
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations when password is valid");
        assertEquals("minhaSenhaNova123", dto.password());
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: invalid when password is shorter than 8 characters")
    void studentUpdateRequest_invalidWhenPasswordTooShort() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                "123",
                null,
                null,
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A senha deve ter entre 8 e 255 caracteres.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("TeacherUpdateRequestDTO: invalid when password exceeds 255 characters")
    void teacherUpdateRequest_invalidWhenPasswordTooLong() {
        TeacherUpdateRequestDTO dto = new TeacherUpdateRequestDTO(
                "Professor Girafales",
                "girafales@escola.com",
                "A".repeat(256)
        );

        Set<ConstraintViolation<TeacherUpdateRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A senha deve ter entre 8 e 255 caracteres.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: invalid when name is blank")
    void studentUpdateRequest_invalidWhenNameBlank() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "   ",
                "chaves@escola.com",
                null,
                null,
                null,
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O nome do aluno é obrigatório.")));
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: invalid when email is invalid format")
    void studentUpdateRequest_invalidWhenEmailInvalid() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "not-an-email",
                null,
                null,
                null,
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O e-mail informado é inválido.")));
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: invalid when registrationNumber is blank")
    void studentUpdateRequest_invalidWhenRegistrationBlank() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                null,
                null,
                null,
                "   ",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().equals("O número de matrícula é obrigatório.")));
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: invalid when birthDate is future")
    void studentUpdateRequest_invalidWhenBirthDateFuture() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                null,
                null,
                null,
                "MAT-12345",
                LocalDate.now().plusDays(1)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A data de nascimento deve ser anterior à data de hoje.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("StudentUpdateRequestDTO: invalid when password exceeds 255 characters")
    void studentUpdateRequest_invalidWhenPasswordTooLong() {
        StudentUpdateRequestDTO dto = new StudentUpdateRequestDTO(
                "Chaves del Ocho",
                "chaves@escola.com",
                "A".repeat(256),
                null,
                null,
                "MAT-12345",
                LocalDate.of(2010, 5, 10)
        );

        Set<ConstraintViolation<StudentUpdateRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A senha deve ter entre 8 e 255 caracteres.", violations.iterator().next().getMessage());
    }
}
