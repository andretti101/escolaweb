package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.StudentPatchRequestDTO;
import com.andretti101.escolaweb.dto.request.TeacherPatchRequestDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.impl.*;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementAndEmailServiceTest {

    @Nested
    @DisplayName("PrincipalServiceImpl & SecretaryServiceImpl Unit Tests")
    class PrincipalAndSecretaryServiceTests {

        @Mock
        private PrincipalRepository principalRepository;
        @Mock
        private SecretaryRepository secretaryRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private MessageSource messageSource;

        @InjectMocks
        private PrincipalServiceImpl principalService;

        @InjectMocks
        private SecretaryServiceImpl secretaryService;

        @BeforeEach
        void setUp() {
            lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Mensagem de erro i18n");
        }

        @Test
        @DisplayName("PrincipalServiceImpl create, update, activate, deactivate e delete funcionam e validam e-mail duplicado")
        void principalLifecycle_validatesEmailAndPasswordPlaceholder() {
            Principal principal = new Principal();
            principal.setName("Helena");
            principal.setEmail("helena@example.com");
            principal.setPassword("senha123");

            // E-mail em uso no create -> IllegalStateException
            when(userRepository.existsByEmail("helena@example.com")).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> principalService.create(principal));

            // Create válido
            when(userRepository.existsByEmail("helena@example.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("hash123");
            when(principalRepository.save(any(Principal.class))).thenAnswer(inv -> inv.getArgument(0));

            Principal created = principalService.create(principal);
            assertTrue(created.isActive());
            assertEquals("hash123", created.getPassword());

            // Update com SenhaTemporaria123 -> mantém senha antiga
            created.setId(1);
            when(principalRepository.findById(1)).thenReturn(Optional.of(created));

            Principal incomingPlaceholder = new Principal();
            incomingPlaceholder.setName("Helena Atualizada");
            incomingPlaceholder.setEmail("helena@example.com");
            incomingPlaceholder.setPassword("SenhaTemporaria123");

            Principal updated = principalService.update(1, incomingPlaceholder);
            assertEquals("Helena Atualizada", updated.getName());
            assertEquals("hash123", updated.getPassword());

            // Update com e-mail de outro usuário -> IllegalStateException
            Principal incomingConflict = new Principal();
            incomingConflict.setName("Helena");
            incomingConflict.setEmail("outro@example.com");
            when(userRepository.existsByEmailAndIdNot("outro@example.com", 1)).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> principalService.update(1, incomingConflict));

            // Deactivate / Activate / Delete
            principalService.deactivate(1);
            assertFalse(created.isActive());
            principalService.activate(1);
            assertTrue(created.isActive());

            principalService.delete(1);
            verify(principalRepository).deleteById(1);

            when(principalRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(EntityNotFoundException.class, () -> principalService.findById(999));
        }

        @Test
        @DisplayName("SecretaryServiceImpl create, update, activate, deactivate e delete funcionam e validam e-mail duplicado")
        void secretaryLifecycle_validatesEmailAndPasswordEncoding() {
            Secretary secretary = new Secretary();
            secretary.setName("Ricardo");
            secretary.setEmail("ricardo@example.com");
            secretary.setPassword("senha123");

            when(userRepository.existsByEmail("ricardo@example.com")).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> secretaryService.create(secretary));

            when(userRepository.existsByEmail("ricardo@example.com")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("secHash");
            when(secretaryRepository.save(any(Secretary.class))).thenAnswer(inv -> inv.getArgument(0));

            Secretary created = secretaryService.create(secretary);
            created.setId(2);
            assertTrue(created.isActive());
            assertEquals("secHash", created.getPassword());

            when(secretaryRepository.findById(2)).thenReturn(Optional.of(created));
            when(passwordEncoder.encode("novaSenha")).thenReturn("novoHash");

            Secretary updateReq = new Secretary();
            updateReq.setName("Ricardo Nogueira");
            updateReq.setEmail("ricardo@example.com");
            updateReq.setPassword("novaSenha");

            Secretary updated = secretaryService.update(2, updateReq);
            assertEquals("Ricardo Nogueira", updated.getName());
            assertEquals("novoHash", updated.getPassword());

            secretaryService.deactivate(2);
            assertFalse(created.isActive());
            secretaryService.activate(2);
            assertTrue(created.isActive());

            secretaryService.delete(2);
            verify(secretaryRepository).deleteById(2);
        }
    }

    @Nested
    @DisplayName("StudentServiceImpl & TeacherServiceImpl Unit Tests")
    class StudentAndTeacherServiceTests {

        @Mock
        private StudentRepository studentRepository;
        @Mock
        private TeacherRepository teacherRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private SubjectRepository subjectRepository;
        @Mock
        private TeacherClassSubjectRepository teacherClassSubjectRepository;
        @Mock
        private PasswordEncoder passwordEncoder;
        @Mock
        private MessageSource messageSource;

        @InjectMocks
        private StudentServiceImpl studentService;

        @InjectMocks
        private TeacherServiceImpl teacherService;

        @BeforeEach
        void setUp() {
            lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Erro validado");
        }

        @Test
        @DisplayName("StudentServiceImpl create, update e patch validam e-mail, matrícula e campos em branco")
        void studentCreateUpdateAndPatch_validatesUniquenessAndBlankFields() {
            Student s = Student.builder()
                    .name("Lucas")
                    .email("lucas@example.com")
                    .password("senha123")
                    .registrationNumber("2026001")
                    .birthDate(LocalDate.of(2010, 5, 10))
                    .build();

            // Matrícula duplicada no create
            when(userRepository.existsByEmail("lucas@example.com")).thenReturn(false);
            when(studentRepository.existsByRegistrationNumber("2026001")).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> studentService.create(s));

            // Create válido
            when(studentRepository.existsByRegistrationNumber("2026001")).thenReturn(false);
            when(passwordEncoder.encode("senha123")).thenReturn("encoded");
            when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

            Student created = studentService.create(s);
            created.setId(11);
            assertTrue(created.isActive());
            assertEquals("encoded", created.getPassword());

            when(studentRepository.findById(11)).thenReturn(Optional.of(created));

            // Patch com nome em branco -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () ->
                    studentService.patch(11, new StudentPatchRequestDTO("   ", null, null, null, null, null, null)));

            // Patch com e-mail em branco -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () ->
                    studentService.patch(11, new StudentPatchRequestDTO(null, "  ", null, null, null, null, null)));

            // Patch com matrícula em branco -> IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () ->
                    studentService.patch(11, new StudentPatchRequestDTO(null, null, null, null, null, " ", null)));

            // Patch válido fazendo trim nos campos
            when(userRepository.existsByEmailAndIdNot("lucas.novo@example.com", 11)).thenReturn(false);
            when(studentRepository.existsByRegistrationNumberAndIdNot("2026999", 11)).thenReturn(false);

            Student patched = studentService.patch(11, new StudentPatchRequestDTO(
                    "  Lucas Almeida  ",
                    "  lucas.novo@example.com  ",
                    "SenhaTemporaria123",
                    "(48) 99999-0000",
                    "(48) 98888-0000",
                    "  2026999  ",
                    LocalDate.of(2010, 6, 15)
            ));

            assertEquals("Lucas Almeida", patched.getName());
            assertEquals("lucas.novo@example.com", patched.getEmail());
            assertEquals("2026999", patched.getRegistrationNumber());
            assertEquals("encoded", patched.getPassword());
        }

        @Test
        @DisplayName("TeacherServiceImpl impede remoção de disciplina com atribuição ativa em turma ativa e impede exclusão de professor com vínculos")
        void teacherUpdatePatchAndDelete_enforcesActiveAssignmentConstraints() {
            Subject math = Subject.builder().id(1).name("Matemática").active(true).build();
            Subject physics = Subject.builder().id(2).name("Física").active(true).build();

            Teacher existing = Teacher.builder()
                    .id(5)
                    .name("Carlos")
                    .email("carlos@example.com")
                    .password("hash")
                    .role(UserRole.TEACHER)
                    .subjects(Set.of(math, physics))
                    .active(true)
                    .build();

            when(teacherRepository.findById(5)).thenReturn(Optional.of(existing));
            when(subjectRepository.findById(1)).thenReturn(Optional.of(math));

            AcademicYear activeYear = AcademicYear.builder().id(2026).year(2026).active(true).build();
            ClassRoom activeRoom = ClassRoom.builder().id(10).name("1º A").academicYear(activeYear).active(true).build();
            TeacherClassSubject activePhysicsTcs = TeacherClassSubject.builder()
                    .id(100)
                    .teacher(existing)
                    .classRoom(activeRoom)
                    .subject(physics)
                    .active(true)
                    .build();

            when(teacherClassSubjectRepository.findByTeacherIdAndActiveTrue(5)).thenReturn(List.of(activePhysicsTcs));

            // Tentar remover Física enquanto leciona em turma ativa do ano ativo -> IllegalStateException
            TeacherPatchRequestDTO removePhysicsDto = new TeacherPatchRequestDTO(null, null, null, List.of(1));
            assertThrows(IllegalStateException.class, () -> teacherService.patch(5, removePhysicsDto));

            // Se o ano letivo da turma estiver inativo (arquivado), permite remover a habilitação!
            activeYear.setActive(false);
            when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));
            Teacher updated = teacherService.patch(5, removePhysicsDto);
            assertEquals(1, updated.getSubjects().size());
            assertTrue(updated.getSubjects().contains(math));

            // Delete de professor com atribuições -> IllegalStateException
            when(teacherClassSubjectRepository.existsByTeacher(existing)).thenReturn(true);
            assertThrows(IllegalStateException.class, () -> teacherService.delete(5));

            // Delete sem atribuições -> sucesso
            when(teacherClassSubjectRepository.existsByTeacher(existing)).thenReturn(false);
            teacherService.delete(5);
            verify(teacherRepository).deleteById(5);
        }
    }

    @Nested
    @DisplayName("EmailServiceImpl Unit Tests")
    class EmailServiceTests {

        @Mock
        private JavaMailSender mailSender;

        @InjectMocks
        private EmailServiceImpl emailService;

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        }

        @Test
        @DisplayName("sendPasswordResetEmail monta MimeMessage com link de redefinição e nome do usuário e envia pelo JavaMailSender")
        void sendPasswordResetEmail_buildsHtmlAndSendsMimeMessage() throws Exception {
            MimeMessage realMimeMessage = new MimeMessage(Session.getInstance(new Properties()));
            when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

            emailService.sendPasswordResetEmail("aluno@example.com", "Lucas Almeida", "tok-abc-123");

            verify(mailSender).send(realMimeMessage);
            assertEquals("EscolaWEB — Redefinição de Senha", realMimeMessage.getSubject());
            assertEquals("aluno@example.com", realMimeMessage.getAllRecipients()[0].toString());
        }

        @Test
        @DisplayName("sendPasswordResetEmail encapsula MessagingException em IllegalStateException")
        void sendPasswordResetEmail_whenMessagingException_throwsIllegalStateException() {
            MimeMessage failingMessage = new MimeMessage(Session.getInstance(new Properties())) {
                @Override
                public void setSubject(String subject, String charset) throws MessagingException {
                    throw new MessagingException("Falha simulada no MIME");
                }
            };
            when(mailSender.createMimeMessage()).thenReturn(failingMessage);

            assertThrows(IllegalStateException.class, () ->
                    emailService.sendPasswordResetEmail("aluno@example.com", "Lucas", "tok-123"));
        }
    }
}
