package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.config.JwtAuthenticationFilter;
import com.andretti101.escolaweb.config.JwtTokenProvider;
import com.andretti101.escolaweb.config.WebSocketAuthInterceptor;
import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.dto.response.StudentAttendanceReportDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.*;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.impl.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAndAiToolsUnitTest {

    @Nested
    @DisplayName("JwtTokenProvider, JwtAuthenticationFilter & WebSocketAuthInterceptor Unit Tests")
    class SecurityAndWebSocketTests {

        @Mock
        private CustomUserDetailsService userDetailsService;
        @Mock
        private EnrollmentRepository enrollmentRepository;
        @Mock
        private UserRepository userRepository;
        @Mock
        private FilterChain filterChain;
        @Mock
        private MessageChannel messageChannel;

        private JwtTokenProvider jwtTokenProvider;
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        private WebSocketAuthInterceptor webSocketAuthInterceptor;
        private UserDetails userDetails;

        @BeforeEach
        void setUp() {
            jwtTokenProvider = new JwtTokenProvider();
            // 256-bit base64 secret
            ReflectionTestUtils.setField(jwtTokenProvider, "secret", "bXlTdXBlclNlY3JldEtleUZvckVzY29sYVdlYkp3dFRva2VuUHJvdmlkZXIyMDI2");
            ReflectionTestUtils.setField(jwtTokenProvider, "expiration", 900_000L);
            ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 604_800_000L);

            jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
            webSocketAuthInterceptor = new WebSocketAuthInterceptor(
                    jwtTokenProvider, userDetailsService, enrollmentRepository, userRepository);

            userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("direcao@example.com")
                    .password("hash")
                    .authorities(new SimpleGrantedAuthority("ROLE_PRINCIPAL"))
                    .build();
        }

        @AfterEach
        void tearDown() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("JwtTokenProvider gera token, extrai username e valida expiração, usuário incorreto e token malformado")
        void jwtTokenProvider_generatesExtractsAndValidates() {
            String token = jwtTokenProvider.generateToken(userDetails);
            assertNotNull(token);
            assertEquals("direcao@example.com", jwtTokenProvider.extractUsername(token));
            assertTrue(jwtTokenProvider.validateToken(token, userDetails));

            UserDetails otherUser = org.springframework.security.core.userdetails.User.builder()
                    .username("outro@example.com")
                    .password("hash")
                    .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                    .build();
            assertFalse(jwtTokenProvider.validateToken(token, otherUser));
            assertFalse(jwtTokenProvider.validateToken("token.invalido.malformado", userDetails));

            // Token expirado
            ReflectionTestUtils.setField(jwtTokenProvider, "expiration", -10_000L);
            String expiredToken = jwtTokenProvider.generateToken(userDetails);
            assertFalse(jwtTokenProvider.validateToken(expiredToken, userDetails));
        }

        @Test
        @DisplayName("JwtAuthenticationFilter autentica requisições com Bearer válido, ignora usuário desabilitado e limpa contexto em token inválido")
        void jwtAuthenticationFilter_handlesValidDisabledAndInvalidTokens() throws Exception {
            String token = jwtTokenProvider.generateToken(userDetails);

            // 1. Sem header Authorization -> segue cadeia sem autenticar
            MockHttpServletRequest reqNoAuth = new MockHttpServletRequest();
            MockHttpServletResponse res = new MockHttpServletResponse();
            jwtAuthenticationFilter.doFilter(reqNoAuth, res, filterChain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());

            // 2. Com Bearer válido e usuário ativo -> autentica no SecurityContext
            MockHttpServletRequest reqValid = new MockHttpServletRequest();
            reqValid.addHeader("Authorization", "Bearer " + token);
            when(userDetailsService.loadUserByUsername("direcao@example.com")).thenReturn(userDetails);

            jwtAuthenticationFilter.doFilter(reqValid, res, filterChain);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals("direcao@example.com", SecurityContextHolder.getContext().getAuthentication().getName());

            // 3. Usuário desabilitado -> não autentica
            SecurityContextHolder.clearContext();
            UserDetails disabledDetails = org.springframework.security.core.userdetails.User.builder()
                    .username("direcao@example.com")
                    .password("hash")
                    .disabled(true)
                    .authorities(new SimpleGrantedAuthority("ROLE_PRINCIPAL"))
                    .build();
            when(userDetailsService.loadUserByUsername("direcao@example.com")).thenReturn(disabledDetails);

            jwtAuthenticationFilter.doFilter(reqValid, res, filterChain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());

            // 4. Token malformado -> captura exceção e mantém contexto limpo
            MockHttpServletRequest reqInvalid = new MockHttpServletRequest();
            reqInvalid.addHeader("Authorization", "Bearer token-corrompido");
            jwtAuthenticationFilter.doFilter(reqInvalid, res, filterChain);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("WebSocketAuthInterceptor autentica no CONNECT e valida permissões de SUBSCRIBE em /topic/teachers e /topic/classroom/{id}")
        void webSocketAuthInterceptor_enforcesConnectAndSubscribeRules() {
            String token = jwtTokenProvider.generateToken(userDetails);
            when(userDetailsService.loadUserByUsername("direcao@example.com")).thenReturn(userDetails);

            // 1. CONNECT com Bearer válido define user no StompHeaderAccessor
            StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
            connectAccessor.addNativeHeader("Authorization", "Bearer " + token);
            connectAccessor.setLeaveMutable(true);
            org.springframework.messaging.Message<byte[]> connectMsg =
                    MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());

            webSocketAuthInterceptor.preSend(connectMsg, messageChannel);
            assertNotNull(connectAccessor.getUser());
            assertEquals("direcao@example.com", connectAccessor.getUser().getName());

            // 2. SUBSCRIBE sem usuário autenticado -> IllegalArgumentException
            StompHeaderAccessor unauthSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            unauthSub.setDestination("/topic/teachers");
            unauthSub.setLeaveMutable(true);
            org.springframework.messaging.Message<byte[]> unauthMsg =
                    MessageBuilder.createMessage(new byte[0], unauthSub.getMessageHeaders());
            assertThrows(IllegalArgumentException.class, () ->
                    webSocketAuthInterceptor.preSend(unauthMsg, messageChannel));

            // 3. SUBSCRIBE de STUDENT em /topic/teachers -> negado
            Student student = Student.builder().id(11).email("aluno@example.com").role(UserRole.STUDENT).build();
            when(userRepository.findByEmail("aluno@example.com")).thenReturn(Optional.of(student));

            StompHeaderAccessor studentTeacherSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            studentTeacherSub.setUser(new UsernamePasswordAuthenticationToken("aluno@example.com", null));
            studentTeacherSub.setDestination("/topic/teachers");
            studentTeacherSub.setLeaveMutable(true);
            org.springframework.messaging.Message<byte[]> studentTeacherMsg =
                    MessageBuilder.createMessage(new byte[0], studentTeacherSub.getMessageHeaders());
            assertThrows(IllegalArgumentException.class, () ->
                    webSocketAuthInterceptor.preSend(studentTeacherMsg, messageChannel));

            // 4. SUBSCRIBE de STUDENT em /topic/classroom/1 (não matriculado vs matriculado)
            StompHeaderAccessor studentClassSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            studentClassSub.setUser(new UsernamePasswordAuthenticationToken("aluno@example.com", null));
            studentClassSub.setDestination("/topic/classroom/1");
            studentClassSub.setLeaveMutable(true);
            org.springframework.messaging.Message<byte[]> studentClassMsg =
                    MessageBuilder.createMessage(new byte[0], studentClassSub.getMessageHeaders());

            when(enrollmentRepository.existsByStudent_IdAndClassRoom_IdAndActiveTrue(11, 1)).thenReturn(false);
            assertThrows(IllegalArgumentException.class, () ->
                    webSocketAuthInterceptor.preSend(studentClassMsg, messageChannel));

            when(enrollmentRepository.existsByStudent_IdAndClassRoom_IdAndActiveTrue(11, 1)).thenReturn(true);
            assertDoesNotThrow(() -> webSocketAuthInterceptor.preSend(studentClassMsg, messageChannel));

            // 5. SUBSCRIBE de TEACHER em /topic/classroom/1 -> negado
            Teacher teacher = Teacher.builder().id(5).email("prof@example.com").role(UserRole.TEACHER).build();
            when(userRepository.findByEmail("prof@example.com")).thenReturn(Optional.of(teacher));

            StompHeaderAccessor teacherClassSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            teacherClassSub.setUser(new UsernamePasswordAuthenticationToken("prof@example.com", null));
            teacherClassSub.setDestination("/topic/classroom/1");
            teacherClassSub.setLeaveMutable(true);
            org.springframework.messaging.Message<byte[]> teacherClassMsg =
                    MessageBuilder.createMessage(new byte[0], teacherClassSub.getMessageHeaders());
            assertThrows(IllegalArgumentException.class, () ->
                    webSocketAuthInterceptor.preSend(teacherClassMsg, messageChannel));
        }
    }

    @Nested
    @DisplayName("JpaChatMemory Unit Tests")
    class JpaChatMemoryTests {

        @Mock
        private ChatMessageHistoryRepository repository;

        @InjectMocks
        private JpaChatMemory jpaChatMemory;

        @Test
        @DisplayName("JpaChatMemory filtra ToolResponseMessage e mensagens vazias no add, recupera últimas N mensagens no get e limpa conversa")
        void jpaChatMemory_addGetAndClear() {
            Message userMsg = new UserMessage("Qual minha média?");
            Message assistantMsg = new AssistantMessage("Sua média é 8.5.");
            Message systemMsg = new SystemMessage("Instrução do sistema");
            Message blankMsg = new UserMessage("   ");
            Message toolResp = mock(ToolResponseMessage.class);

            jpaChatMemory.add("conv-1", List.of(userMsg, assistantMsg, systemMsg, blankMsg, toolResp));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ChatMessageHistory>> captor = ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());
            List<ChatMessageHistory> saved = captor.getValue();
            assertEquals(3, saved.size());
            assertEquals("USER", saved.get(0).getMessageType());
            assertEquals("ASSISTANT", saved.get(1).getMessageType());
            assertEquals("SYSTEM", saved.get(2).getMessageType());

            when(repository.findByConversationIdOrderByCreatedAtAsc("conv-1")).thenReturn(saved);
            List<Message> lastTwo = jpaChatMemory.get("conv-1", 2);
            assertEquals(2, lastTwo.size());
            assertInstanceOf(AssistantMessage.class, lastTwo.get(0));
            assertInstanceOf(SystemMessage.class, lastTwo.get(1));

            jpaChatMemory.clear("conv-1");
            verify(repository).deleteByConversationId("conv-1");
        }
    }

    @Nested
    @DisplayName("StudentAiTools & TeacherAiTools Unit Tests")
    class StudentAndTeacherAiToolsTests {

        @Mock
        private TransactionTemplate transactionTemplate;
        @Mock
        private AuthenticatedUserService authService;
        @Mock
        private GradeService gradeService;
        @Mock
        private AttendanceService attendanceService;
        @Mock
        private ReportCardService reportCardService;
        @Mock
        private AcademicYearService academicYearService;
        @Mock
        private EnrollmentService enrollmentService;
        @Mock
        private TeacherClassSubjectService teacherClassSubjectService;
        @Mock
        private AcademicPeriodService academicPeriodService;
        @Mock
        private SchoolSettingsService schoolSettingsService;
        @Mock
        private StudentRepository studentRepository;
        @Mock
        private AssessmentRepository assessmentRepository;
        @Mock
        private GradeRepository gradeRepository;
        @Mock
        private TeacherService teacherService;

        @InjectMocks
        private StudentAiTools studentAiTools;

        @InjectMocks
        private TeacherAiTools teacherAiTools;

        @BeforeEach
        void setUp() {
            lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> callback = inv.getArgument(0);
                return callback.doInTransaction(mock(TransactionStatus.class));
            });
        }

        @Test
        @DisplayName("StudentAiTools retorna notas, boletim, pontos necessários, colegas, matrícula e análise de matérias")
        void studentAiTools_executesToolsCorrectly() {
            Student student = Student.builder().id(11).name("Lucas").active(true).build();
            Student colleague = Student.builder().id(12).name("Beatriz").active(true).build();
            AcademicYear year = AcademicYear.builder().id(1).year(2026).startDate(LocalDate.of(2026, 2, 1)).endDate(LocalDate.of(2026, 12, 15)).active(true).build();
            ClassRoom cr = ClassRoom.builder().id(10).name("1º A").schoolGrade(SchoolGrade.MEDIO_1).shift(Shift.MORNING).active(true).build();
            Subject math = Subject.builder().id(1).name("Matemática").build();
            Subject hist = Subject.builder().id(2).name("História").build();
            TeacherClassSubject tcs = TeacherClassSubject.builder().id(100).classRoom(cr).subject(math).teacher(Teacher.builder().name("Mariana").build()).build();
            Assessment a1 = Assessment.builder().title("Prova 1").teacherClassSubject(tcs).build();
            Grade g1 = Grade.builder().student(student).assessment(a1).value(new BigDecimal("8.5")).build();

            when(authService.getAuthenticatedStudent()).thenReturn(student);
            when(gradeService.findByStudent(11)).thenReturn(List.of(g1));
            assertTrue(studentAiTools.getStudentGrades().contains("Matemática - Avaliação: Prova 1, Nota: 8.5"));

            when(academicYearService.findActive()).thenReturn(year);
            ReportCardDTO.SubjectSituationDTO subMath = new ReportCardDTO.SubjectSituationDTO(
                    100, "Matemática", List.of(), new BigDecimal("18.0"), new BigDecimal("18.0"), new BigDecimal("30.0"), StudentSituation.APPROVED);
            ReportCardDTO.SubjectSituationDTO subHist = new ReportCardDTO.SubjectSituationDTO(
                    101, "História", List.of(), new BigDecimal("12.0"), new BigDecimal("18.0"), new BigDecimal("30.0"), StudentSituation.PENDING);
            ReportCardDTO reportCard = new ReportCardDTO(
                    11, "Lucas", "2026001", 1, 2026, "1º A", AcademicPeriodType.TRIMESTER, "Trimestre",
                    new BigDecimal("6.0"), 3, new BigDecimal("18.0"), List.of(subMath, subHist),
                    new ReportCardDTO.FrequencySituationDTO(new BigDecimal("90.00"), new BigDecimal("75.00"), StudentSituation.APPROVED),
                    StudentSituation.PENDING);

            when(reportCardService.generateReportCard(11, 1)).thenReturn(reportCard);
            assertTrue(studentAiTools.getStudentReportCard().contains("Boletim Escolar do aluno Lucas"));

            String pointsNeeded = studentAiTools.getStudentPointsNeeded();
            assertTrue(pointsNeeded.contains("Matemática: Aprovado"));
            assertTrue(pointsNeeded.contains("História: Precisa de 6.0 pontos."));

            when(attendanceService.getStudentAttendanceReport(11, 1)).thenReturn(
                    new StudentAttendanceReportDTO(11, new BigDecimal("90.00"),
                            Map.of("Matemática", 0, "História", 2),
                            Map.of("Matemática", new BigDecimal("100.00"), "História", new BigDecimal("80.00"))));
            String analysis = studentAiTools.getStudentSubjectAnalysis();
            assertTrue(analysis.contains("Melhor matéria em notas: Matemática"));
            assertTrue(analysis.contains("Pior matéria em notas: História"));

            Enrollment e1 = Enrollment.builder().student(student).classRoom(cr).active(true).build();
            Enrollment e2 = Enrollment.builder().student(colleague).classRoom(cr).active(true).build();
            when(enrollmentService.findByStudent(11)).thenReturn(List.of(e1));
            when(enrollmentService.findByClassRoom(10)).thenReturn(List.of(e1, e2));

            assertTrue(studentAiTools.getStudentClassmates().contains("Beatriz"));
            assertTrue(studentAiTools.getStudentEnrollmentDetails().contains("1º A"));

            when(teacherClassSubjectService.findByClassRoom(10)).thenReturn(List.of(tcs));
            assertTrue(studentAiTools.getStudentTeachers().contains("Mariana"));
        }

        @Test
        @DisplayName("TeacherAiTools lista turmas, valida acesso aos alunos da turma, identifica baixo desempenho e avaliações pendentes")
        void teacherAiTools_executesToolsAndValidatesClassroomAccess() {
            Teacher teacher = Teacher.builder().id(5).name("Carlos").active(true).build();
            Teacher colleague = Teacher.builder().id(6).name("Patrícia").active(true).build();
            AcademicYear year = AcademicYear.builder().id(1).year(2026).active(true).build();
            AcademicPeriod p1 = AcademicPeriod.builder().id(10).name("1º Trimestre").build();
            ClassRoom cr = ClassRoom.builder().id(20).name("2º A").build();
            Subject physics = Subject.builder().id(3).name("Física").build();
            TeacherClassSubject tcs = TeacherClassSubject.builder()
                    .id(100)
                    .teacher(teacher)
                    .classRoom(cr)
                    .subject(physics)
                    .minAssessmentsPerPeriod(2)
                    .build();

            when(authService.getAuthenticatedTeacher()).thenReturn(teacher);
            when(teacherClassSubjectService.findByTeacher(5)).thenReturn(List.of(tcs));

            assertTrue(teacherAiTools.getTeacherClasses().contains("Turma: 2º A - Matéria: Física"));
            assertEquals("Acesso negado: Você não leciona nesta turma.", teacherAiTools.getTeacherClassStudents(99));

            Student s = Student.builder().id(11).name("Lucas").build();
            when(studentRepository.findByClassroomId(20)).thenReturn(List.of(s));
            assertTrue(teacherAiTools.getTeacherClassStudents(20).contains("Lucas"));

            Assessment a = Assessment.builder().id(50).title("Prova 1").teacherClassSubject(tcs).build();
            Grade lowGrade = Grade.builder().student(s).assessment(a).value(new BigDecimal("4.5")).build();
            when(assessmentRepository.findByTeacherClassSubject(tcs)).thenReturn(List.of(a));
            when(gradeRepository.findByAssessment(a)).thenReturn(List.of(lowGrade));

            String perf = teacherAiTools.getTeacherStudentPerformance();
            assertTrue(perf.contains("Lucas: 4.5"));

            when(academicYearService.findActive()).thenReturn(year);
            when(academicPeriodService.findByAcademicYear(1)).thenReturn(List.of(p1));
            when(assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, p1)).thenReturn(1L);

            String pending = teacherAiTools.getTeacherPendingAssessments();
            assertTrue(pending.contains("Pendentes: 1 avaliações."));

            when(teacherService.findAllActive()).thenReturn(List.of(teacher, colleague));
            assertTrue(teacherAiTools.getOtherTeachers().contains("Patrícia"));
        }

        @Test
        @DisplayName("TeacherAiTools gera plano de aula BNCC e rascunho de avaliação com gabarito comentado")
        void teacherAiTools_generatesLessonPlanAndExamQuestions() {
            SchoolSettings settings = SchoolSettings.builder().schoolName("Escola Modelo").build();
            when(schoolSettingsService.findSettings()).thenReturn(settings);

            // 1. Plano de Aula
            String lessonPlan = teacherAiTools.suggestLessonPlan(
                    "Biologia",
                    "7º Ano Fundamental",
                    "Fotossíntese",
                    "Compreender a conversão de energia luminosa em energia química"
            );
            assertNotNull(lessonPlan);
            assertTrue(lessonPlan.contains("PROPOSTA DE PLANO DE AULA (ALINHADO À BNCC)"));
            assertTrue(lessonPlan.contains("Escola Modelo"));
            assertTrue(lessonPlan.contains("Biologia"));
            assertTrue(lessonPlan.contains("7º Ano Fundamental"));
            assertTrue(lessonPlan.contains("Fotossíntese"));
            assertTrue(lessonPlan.contains("COMPETÊNCIAS E HABILIDADES BNCC"));
            assertTrue(lessonPlan.contains("DESENVOLVIMENTO METODOLÓGICO"));

            // 2. Questões de Avaliação (Múltipla Escolha)
            String multipleChoice = teacherAiTools.generateExamQuestions(
                    "Biologia",
                    "Fotossíntese",
                    3,
                    "MÚLTIPLA_ESCOLHA"
            );
            assertNotNull(multipleChoice);
            assertTrue(multipleChoice.contains("RASCUNHO DE QUESTÕES DE AVALIAÇÃO COM GABARITO COMENTADO"));
            assertTrue(multipleChoice.contains("Questão 1"));
            assertTrue(multipleChoice.contains("Questão 3"));
            assertTrue(multipleChoice.contains("Alternativa Correta: C"));
            assertTrue(multipleChoice.contains("Justificativa Pedagógica"));

            // 3. Questões de Avaliação (Discursiva)
            String discursive = teacherAiTools.generateExamQuestions(
                    "História",
                    "Revolução Francesa",
                    2,
                    "DISCURSIVA"
            );
            assertNotNull(discursive);
            assertTrue(discursive.contains("Revolução Francesa"));
            assertTrue(discursive.contains("Critérios de Pontuação"));
            assertTrue(discursive.contains("Expectativa de Resposta"));
        }
    }
}
