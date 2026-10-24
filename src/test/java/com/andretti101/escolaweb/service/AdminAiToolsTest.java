package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.*;
import com.andretti101.escolaweb.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AdminAiToolsTest {

    @Autowired
    private AdminAiTools adminAiTools;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AcademicYear activeYear;
    private Subject testSubject;
    private Teacher activeTeacher;
    private ClassRoom activeClassRoom;

    @BeforeEach
    void setUp() {
        activeYear = academicYearRepository.findByActiveTrue().orElseGet(() ->
                academicYearRepository.save(AcademicYear.builder()
                        .year(2026)
                        .startDate(LocalDate.of(2026, 2, 1))
                        .endDate(LocalDate.of(2026, 12, 15))
                        .minimumGrade(BigDecimal.valueOf(6.0))
                        .minimumAttendance(BigDecimal.valueOf(75.0))
                        .active(true)
                        .build()));

        testSubject = subjectRepository.save(Subject.builder()
                .name("Filosofia IA Teste")
                .active(true)
                .build());

        activeTeacher = teacherRepository.save(Teacher.builder()
                .name("Prof AI Ativo")
                .email("prof.ai.ativo@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .subjects(Set.of(testSubject))
                .active(true)
                .build());

        activeClassRoom = classRoomRepository.save(ClassRoom.builder()
                .name("Turma IA Ativa 101")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("getTeacherCountBySubject com matéria específica distingue lecionando ativamente de habilitados")
    void testGetTeacherCountBySubject_specificSubject() {
        // Inicialmente: 1 habilitado no cadastro (activeTeacher), 0 lecionando ativamente
        String resultInitial = adminAiTools.getTeacherCountBySubject("Filosofia IA Teste");
        assertTrue(resultInitial.contains("Professores lecionando ativamente: 0"));
        assertTrue(resultInitial.contains("Professores habilitados no cadastro: 1"));

        // Agora vincula na turma ativa
        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(activeTeacher)
                .classRoom(activeClassRoom)
                .subject(testSubject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        String resultAfterAssign = adminAiTools.getTeacherCountBySubject("Filosofia IA Teste");
        assertTrue(resultAfterAssign.contains("Professores lecionando ativamente: 1"));
        assertTrue(resultAfterAssign.contains("Professores habilitados no cadastro: 1"));
    }

    @Test
    @DisplayName("getTeacherCountBySubject ignora turmas inativas e professores inativos na contagem de ativamente lecionando")
    void testGetTeacherCountBySubject_ignoresInactiveEntities() {
        Teacher inactiveTeacher = teacherRepository.save(Teacher.builder()
                .name("Prof AI Inativo")
                .email("prof.ai.inativo@example.com")
                .password(passwordEncoder.encode("senha123"))
                .role(UserRole.TEACHER)
                .subjects(Set.of(testSubject))
                .active(false) // professor desativado
                .build());

        ClassRoom inactiveClass = classRoomRepository.save(ClassRoom.builder()
                .name("Turma IA Inativa 102")
                .shift(Shift.MORNING)
                .periodType(AcademicPeriodType.TRIMESTER)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(activeYear)
                .active(false) // turma desativada
                .build());

        // TCS atribuído ao professor inativo
        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(inactiveTeacher)
                .classRoom(activeClassRoom)
                .subject(testSubject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        // TCS atribuído ao professor ativo em turma inativa
        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .teacher(activeTeacher)
                .classRoom(inactiveClass)
                .subject(testSubject)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        // Não deve contar nem professor inativo nem turma inativa como lecionando ativamente
        String res = adminAiTools.getTeacherCountBySubject("Filosofia IA Teste");
        assertTrue(res.contains("Professores lecionando ativamente: 0"));
        // E no cadastro, somente o professor ativo é contabilizado como habilitado (1 ativo, 1 inativo)
        assertTrue(res.contains("Professores habilitados no cadastro: 1"));
    }

    @Test
    @DisplayName("getTeacherCountBySubject sem parâmetro lista todas as matérias com distinção semântica")
    void testGetTeacherCountBySubject_allSubjects() {
        String res = adminAiTools.getTeacherCountBySubject(null);
        assertNotNull(res);
        assertTrue(res.contains("Filosofia IA Teste"));
        assertTrue(res.contains("Lecionando ativamente:"));
        assertTrue(res.contains("Habilitados no cadastro:"));
    }

    @Test
    @DisplayName("getTeacherCountBySubject com matéria inexistente retorna mensagem informativa")
    void testGetTeacherCountBySubject_notFound() {
        String res = adminAiTools.getTeacherCountBySubject("Matéria Fantasma 9999");
        assertTrue(res.contains("Matéria não encontrada"));
    }

    @Test
    @DisplayName("executeFlexibleSql bloqueia comandos maliciosos DDL e DML")
    void testExecuteFlexibleSql_blocksMaliciousCommands() {
        String dropResult = adminAiTools.executeFlexibleSql("DROP TABLE students");
        assertTrue(dropResult.contains("Erro de segurança"), "Deveria bloquear DROP TABLE: " + dropResult);

        String deleteResult = adminAiTools.executeFlexibleSql("DELETE FROM grades");
        assertTrue(deleteResult.contains("Erro de segurança"), "Deveria bloquear DELETE: " + deleteResult);

        String insertResult = adminAiTools.executeFlexibleSql("INSERT INTO users (email) VALUES ('hacker@test.com')");
        assertTrue(insertResult.contains("Erro de segurança"), "Deveria bloquear INSERT: " + insertResult);

        String chainedResult = adminAiTools.executeFlexibleSql("SELECT * FROM users; DROP TABLE students");
        assertTrue(chainedResult.contains("Erro de segurança"), "Deveria bloquear comandos múltiplos: " + chainedResult);

        String selectIntoResult = adminAiTools.executeFlexibleSql("SELECT * INTO evil_backup FROM users");
        assertTrue(selectIntoResult.contains("Erro de segurança"), "Deveria bloquear SELECT INTO: " + selectIntoResult);

        String lockResult = adminAiTools.executeFlexibleSql("LOCK TABLE users IN EXCLUSIVE MODE");
        assertTrue(lockResult.contains("Erro de segurança"), "Deveria bloquear LOCK: " + lockResult);

        String truncateResult = adminAiTools.executeFlexibleSql("TRUNCATE TABLE grades");
        assertTrue(truncateResult.contains("Erro de segurança"), "Deveria bloquear TRUNCATE: " + truncateResult);
    }

    @Test
    @DisplayName("executeFlexibleSql executa SELECT estrito com sucesso")
    void testExecuteFlexibleSql_allowsSelectQuery() {
        String result = adminAiTools.executeFlexibleSql("SELECT count(*) as total FROM users");
        assertNotNull(result);
        assertFalse(result.contains("Erro de segurança"));
        assertTrue(result.contains("total"));
    }

    @Test
    @DisplayName("draftAnnouncement gera minuta estruturada de comunicado escolar")
    void testDraftAnnouncement_generatesStructuredDraft() {
        String draft = adminAiTools.draftAnnouncement(
                "Reunião de Pais e Mestres",
                "Pais e Responsáveis",
                "Apresentação do calendário acadêmico; Entrega dos boletins do 1º trimestre",
                "Acolhedor e informativo"
        );
        assertNotNull(draft);
        assertTrue(draft.contains("MINUTA DE COMUNICADO OFICIAL"));
        assertTrue(draft.contains("Reunião de Pais e Mestres"));
        assertTrue(draft.contains("Pais e Responsáveis"));
        assertTrue(draft.contains("Apresentação do calendário acadêmico"));
        assertTrue(draft.contains("Direção Escolar"));
    }

    @Test
    @DisplayName("executeFlexibleSql bloqueia acesso a tabelas e colunas sensíveis")
    void testExecuteFlexibleSql_blocksSensitiveTablesAndColumns() {
        String passResult = adminAiTools.executeFlexibleSql("SELECT id, password FROM users");
        assertTrue(passResult.contains("Erro de segurança"), "Deveria bloquear coluna password: " + passResult);

        String rtResult = adminAiTools.executeFlexibleSql("SELECT * FROM refresh_tokens");
        assertTrue(rtResult.contains("Erro de segurança"), "Deveria bloquear refresh_tokens: " + rtResult);

        String prtResult = adminAiTools.executeFlexibleSql("SELECT * FROM password_reset_tokens");
        assertTrue(prtResult.contains("Erro de segurança"), "Deveria bloquear password_reset_tokens: " + prtResult);

        String sleepResult = adminAiTools.executeFlexibleSql("SELECT pg_sleep(5)");
        assertTrue(sleepResult.contains("Erro de segurança"), "Deveria bloquear pg_sleep: " + sleepResult);
    }

    @Test
    @DisplayName("getDatabaseSchema oculta tabelas sensíveis e coluna password")
    void testGetDatabaseSchema_filtersSensitiveTablesAndColumns() {
        String schema = adminAiTools.getDatabaseSchema();
        assertNotNull(schema);
        assertFalse(schema.toLowerCase().contains("password_reset_tokens"));
        assertFalse(schema.toLowerCase().contains("refresh_tokens"));
        assertFalse(schema.toLowerCase().contains("chat_message_history"));
        assertFalse(schema.toLowerCase().contains("flyway_schema_history"));
        assertFalse(schema.toLowerCase().contains("{table_name=users, column_name=password"));
    }
}
