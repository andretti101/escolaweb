package com.andretti101.escolaweb.dto;

import com.andretti101.escolaweb.dto.request.AcademicPeriodRequestDTO;
import com.andretti101.escolaweb.dto.request.ClassRoomRequestDTO;
import com.andretti101.escolaweb.dto.request.GradeLaunchRequestDTO;
import com.andretti101.escolaweb.dto.request.GradeUpdateRequestDTO;
import com.andretti101.escolaweb.dto.request.SchoolSettingsRequestDTO;
import com.andretti101.escolaweb.dto.request.StudentPatchRequestDTO;
import com.andretti101.escolaweb.dto.request.StudentUpdateRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherPatchRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherUpdateRequestDTO;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.SchoolGrade;
import com.andretti101.escolaweb.model.enums.Shift;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    @DisplayName("GradeUpdateRequestDTO: valid with notes and trims whitespace")
    void gradeUpdateRequest_validWithNotes() {
        GradeUpdateRequestDTO dto = new GradeUpdateRequestDTO(
                new BigDecimal("8.5"),
                "  Nota revisada após recurso do aluno  "
        );

        Set<ConstraintViolation<GradeUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
        assertEquals("Nota revisada após recurso do aluno", dto.notes());
    }

    @Test
    @DisplayName("GradeUpdateRequestDTO: normalizes blank notes to null")
    void gradeUpdateRequest_blankNotesNormalizedToNull() {
        GradeUpdateRequestDTO dto = new GradeUpdateRequestDTO(
                new BigDecimal("7.0"),
                "   "
        );

        Set<ConstraintViolation<GradeUpdateRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
        assertNull(dto.notes());
    }

    @Test
    @DisplayName("GradeUpdateRequestDTO: invalid when notes exceed 500 characters")
    void gradeUpdateRequest_invalidWhenNotesTooLong() {
        GradeUpdateRequestDTO dto = new GradeUpdateRequestDTO(
                new BigDecimal("9.0"),
                "X".repeat(501)
        );

        Set<ConstraintViolation<GradeUpdateRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A observação deve ter no máximo 500 caracteres.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("ClassRoomRequestDTO: invalid when schoolGrade is null")
    void classRoomRequest_invalidWhenSchoolGradeNull() {
        ClassRoomRequestDTO dto = new ClassRoomRequestDTO(
                1,
                "1º Ano A",
                Shift.MORNING,
                AcademicPeriodType.TRIMESTER,
                null
        );

        Set<ConstraintViolation<ClassRoomRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("A série é obrigatória.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("ClassRoomRequestDTO: valid when schoolGrade is provided")
    void classRoomRequest_validWhenSchoolGradeProvided() {
        ClassRoomRequestDTO dto = new ClassRoomRequestDTO(
                1,
                "1º Ano A",
                Shift.MORNING,
                AcademicPeriodType.TRIMESTER,
                SchoolGrade.MEDIO_1
        );

        Set<ConstraintViolation<ClassRoomRequestDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("ClassRoomRequestDTO: invalid when periodType is null")
    void classRoomRequest_invalidWhenPeriodTypeNull() {
        ClassRoomRequestDTO dto = new ClassRoomRequestDTO(
                1,
                "1º Ano A",
                Shift.MORNING,
                null,
                SchoolGrade.MEDIO_1
        );

        Set<ConstraintViolation<ClassRoomRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("O tipo de divisão é obrigatório.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("AcademicPeriodRequestDTO: invalid when periodType is null")
    void academicPeriodRequest_invalidWhenPeriodTypeNull() {
        AcademicPeriodRequestDTO dto = new AcademicPeriodRequestDTO(
                1,
                null,
                "1º Trimestre",
                LocalDate.now(),
                LocalDate.now().plusMonths(3)
        );

        Set<ConstraintViolation<AcademicPeriodRequestDTO>> violations = validator.validate(dto);
        assertEquals(1, violations.size());
        assertEquals("O tipo de divisão é obrigatório.", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("TeacherRequestDTO: valid with zero, one, or multiple subjects")
    void teacherRequest_validWithOptionalSubjectIds() {
        TeacherRequestDTO withoutSubjects = new TeacherRequestDTO(
                "Prof Sem Disciplina",
                "sem.disc@escola.com",
                "senha12345",
                null
        );
        TeacherRequestDTO emptySubjects = new TeacherRequestDTO(
                "Prof Lista Vazia",
                "vazia.disc@escola.com",
                "senha12345",
                List.of()
        );
        TeacherRequestDTO multiSubjects = new TeacherRequestDTO(
                "Prof Multi Disciplinas",
                "multi.disc@escola.com",
                "senha12345",
                List.of(1, 2, 3)
        );

        assertTrue(validator.validate(withoutSubjects).isEmpty());
        assertTrue(validator.validate(emptySubjects).isEmpty());
        assertTrue(validator.validate(multiSubjects).isEmpty());
    }

    @Test
    @DisplayName("TeacherPatchRequestDTO: valid when all fields are null (empty patch)")
    void teacherPatchRequest_validWhenAllFieldsNull() {
        TeacherPatchRequestDTO dto = new TeacherPatchRequestDTO(null, null, null, null);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("TeacherPatchRequestDTO: invalid when name is blank whitespace")
    void teacherPatchRequest_invalidWhenNameBlank() {
        TeacherPatchRequestDTO dto = new TeacherPatchRequestDTO("   ", null, null, null);
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("TeacherPatchRequestDTO: invalid when email is blank or malformed")
    void teacherPatchRequest_invalidWhenEmailMalformed() {
        TeacherPatchRequestDTO blankEmail = new TeacherPatchRequestDTO(null, "", null, null);
        assertFalse(validator.validate(blankEmail).isEmpty());

        TeacherPatchRequestDTO malformedEmail = new TeacherPatchRequestDTO(null, "invalido", null, null);
        assertFalse(validator.validate(malformedEmail).isEmpty());
    }

    @Test
    @DisplayName("TeacherPatchRequestDTO: normalizes empty/blank password to null")
    void teacherPatchRequest_normalizesBlankPassword() {
        TeacherPatchRequestDTO dto = new TeacherPatchRequestDTO(null, null, "   ", null);
        assertNull(dto.password());
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("TeacherPatchRequestDTO: invalid when password is shorter than 8 chars")
    void teacherPatchRequest_invalidWhenPasswordTooShort() {
        TeacherPatchRequestDTO dto = new TeacherPatchRequestDTO(null, null, "12345", null);
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("StudentPatchRequestDTO: valid when all fields are null")
    void studentPatchRequest_validWhenAllFieldsNull() {
        StudentPatchRequestDTO dto = new StudentPatchRequestDTO(null, null, null, null, null, null, null);
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("StudentPatchRequestDTO: invalid when name is blank whitespace")
    void studentPatchRequest_invalidWhenNameBlank() {
        StudentPatchRequestDTO dto = new StudentPatchRequestDTO("   ", null, null, null, null, null, null);
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("StudentPatchRequestDTO: invalid when birthDate is in future")
    void studentPatchRequest_invalidWhenBirthDateInFuture() {
        StudentPatchRequestDTO dto = new StudentPatchRequestDTO(null, null, null, null, null, null, LocalDate.now().plusDays(1));
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("StudentPatchRequestDTO: invalid when registrationNumber is blank")
    void studentPatchRequest_invalidWhenRegistrationNumberBlank() {
        StudentPatchRequestDTO dto = new StudentPatchRequestDTO(null, null, null, null, null, "   ", null);
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("SchoolSettingsRequestDTO: valid with cep and standard fields")
    void schoolSettingsRequest_validWithCep() {
        SchoolSettingsRequestDTO dto = new SchoolSettingsRequestDTO(
                "Escola Modelo",
                "88801-010",
                "Rua das Acácias, 120",
                "(48) 3433-0000",
                "contato@escola.com",
                new BigDecimal("6.0"),
                new BigDecimal("75.0")
        );
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    @DisplayName("SchoolSettingsRequestDTO: invalid when cep exceeds 20 characters")
    void schoolSettingsRequest_invalidWhenCepTooLong() {
        SchoolSettingsRequestDTO dto = new SchoolSettingsRequestDTO(
                "Escola Modelo",
                "123456789012345678901", // 21 chars
                "Rua das Acácias, 120",
                "(48) 3433-0000",
                "contato@escola.com",
                new BigDecimal("6.0"),
                new BigDecimal("75.0")
        );
        Set<ConstraintViolation<SchoolSettingsRequestDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("cep")));
    }
}

