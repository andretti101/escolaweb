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
}
