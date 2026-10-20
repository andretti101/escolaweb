package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.YearConclusionRequestDTO;
import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class YearConclusionAuditIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AcademicYearService academicYearService;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private ReportCardService reportCardService;

    @Autowired
    private AcademicYearConclusionService academicYearConclusionService;

    @BeforeEach
    void resetDatabaseWithSeed() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }
    }

    @Test
    @DisplayName("4º Bimestre (ID 21) deve conter avaliações e notas para os alunos da turma 8 (M2-A)")
    void testPeriod21AssessmentsAndGradesExistForCohortB() {
        AcademicPeriod p21 = academicPeriodRepository.findById(21).orElseThrow();
        assertEquals("4º Bimestre", p21.getName());
        assertEquals(AcademicPeriodType.BIMESTER, p21.getPeriodType());

        // Alunos da turma 8 (M2-A)
        List<Integer> cohortBStudentIds = List.of(21, 22, 23, 24, 25, 26, 27, 28, 29, 30);

        for (Integer studentId : cohortBStudentIds) {
            List<Grade> gradesP21 = gradeRepository.findAll().stream()
                    .filter(g -> g.getStudent().getId().equals(studentId))
                    .filter(g -> g.getAssessment().getPeriod().getId().equals(21))
                    .toList();
            assertFalse(gradesP21.isEmpty(), "Aluno " + studentId + " deve ter notas no 4º Bimestre (período 21)");
        }
    }

    @Test
    @DisplayName("Quando todos os períodos são fechados, nenhum aluno da turma 8 ou do ano ativo deve ficar PENDING no boletim")
    void testNoPendingWhenAllPeriodsClosed() {
        AcademicYear activeYear = academicYearService.findActive();

        // Fechar todos os períodos do ano ativo
        List<AcademicPeriod> periods = academicPeriodRepository.findByAcademicYear(activeYear);
        for (AcademicPeriod p : periods) {
            p.setClosed(true);
            academicPeriodRepository.save(p);
        }

        List<Enrollment> enrollments = enrollmentRepository.findByClassRoom_AcademicYearAndActiveTrue(activeYear);
        List<ReportCardDTO> reportCards = reportCardService.generateReportCardsBatch(enrollments, activeYear);

        assertFalse(reportCards.isEmpty());
        for (ReportCardDTO rc : reportCards) {
            assertNotEquals(StudentSituation.PENDING, rc.generalSituation(),
                    "Aluno ID " + rc.studentId() + " (" + rc.studentName() + ") não pode estar PENDING quando todos os períodos estão fechados");
            for (ReportCardDTO.SubjectSituationDTO subj : rc.subjects()) {
                assertNotEquals(StudentSituation.PENDING, subj.situation(),
                        "Disciplina " + subj.subjectName() + " para aluno ID " + rc.studentId() + " não pode estar PENDING quando todos os períodos estão fechados");
            }
        }
    }

    @Test
    @DisplayName("Conclusão do ano letivo deve salvar todas as matrículas como APPROVED ou FAILED, com ZERO PENDING")
    void testConcludeYearLeavesZeroPending() {
        AcademicYear activeYear = academicYearService.findActive();

        // Fechar todos os períodos para permitir conclusão
        List<AcademicPeriod> periods = academicPeriodRepository.findByAcademicYear(activeYear);
        for (AcademicPeriod p : periods) {
            p.setClosed(true);
            academicPeriodRepository.save(p);
        }

        assertTrue(academicYearConclusionService.canConcludeYear());

        YearConclusionRequestDTO request = new YearConclusionRequestDTO(
                2027,
                new BigDecimal("6.0"),
                new BigDecimal("75.0")
        );

        Map<String, Object> result = academicYearConclusionService.concludeYear(request);
        assertNotNull(result);
        assertEquals(41, result.get("approvedStudents"));
        assertEquals(19, result.get("failedStudents"));
        assertEquals(60, result.get("totalStudentsProcessed"));

        // Verificar matrículas concluídas do ano 2026
        List<Enrollment> concludedEnrollments = enrollmentRepository.findByClassRoom_AcademicYear(activeYear);
        assertFalse(concludedEnrollments.isEmpty());

        for (Enrollment enrollment : concludedEnrollments) {
            StudentSituation sit = enrollment.getGeneralSituation();
            assertNotNull(sit, "Situação não pode ser nula");
            assertNotEquals(StudentSituation.PENDING, sit,
                    "Matrícula do aluno " + enrollment.getStudent().getName() + " não pode ter situação PENDING após conclusão");
            assertTrue(sit == StudentSituation.APPROVED || sit == StudentSituation.FAILED,
                    "Situação deve ser APPROVED ou FAILED, obtido: " + sit);
        }

        // Alunos específicos destacados no requisito: notas devidamente computadas e sem pendências
        for (Enrollment enrollment : concludedEnrollments) {
            int sid = enrollment.getStudent().getId();
            if (sid == 28) {
                // Bianca Rocha (28): reprovada por nota (médias ~5.0 nos períodos fechados e ~3.0 no 4º bimestre)
                assertEquals(StudentSituation.FAILED, enrollment.getGeneralSituation(),
                        "Bianca Rocha (ID 28) deve ser FAILED na conclusão");
            } else if (Set.of(24, 25, 26, 30).contains(sid)) {
                // Júlia (24), Vinícius (25), Mariana (26), Natália (30): aprovados
                assertEquals(StudentSituation.APPROVED, enrollment.getGeneralSituation(),
                        "Aluno ID " + sid + " deve ser APPROVED na conclusão");
            }
        }
    }
}
