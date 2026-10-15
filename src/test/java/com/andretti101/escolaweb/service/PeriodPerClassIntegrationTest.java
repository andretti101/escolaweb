package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.dto.request.SchoolSettingsRequestDTO;
import com.andretti101.escolaweb.dto.response.SchoolSettingsResponseDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.*;
import com.andretti101.escolaweb.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PeriodPerClassIntegrationTest {

    @Autowired
    private AcademicPeriodService academicPeriodService;

    @Autowired
    private AcademicYearService academicYearService;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private AcademicPeriodRepository academicPeriodRepository;

    @Autowired
    private ClassRoomService classRoomService;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private TeacherClassSubjectRepository teacherClassSubjectRepository;

    @Autowired
    private AcademicYearConclusionService academicYearConclusionService;

    @Autowired
    private SchoolSettingsService schoolSettingsService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("createDivision deve gerar número correto de períodos e bloquear duplicata")
    void testCreateDivision() {
        AcademicYear testYear = academicYearRepository.save(
                AcademicYear.builder()
                        .year(2099)
                        .startDate(LocalDate.of(2099, 2, 1))
                        .endDate(LocalDate.of(2099, 12, 15))
                        .minimumGrade(BigDecimal.valueOf(6.0))
                        .minimumAttendance(BigDecimal.valueOf(75.0))
                        .active(false)
                        .build()
        );

        AcademicPeriodType type = AcademicPeriodType.SEMESTER;

        List<AcademicPeriod> periods = academicPeriodService.createDivision(testYear.getId(), type);
        assertEquals(2, periods.size(), "Semestre deve gerar 2 períodos");
        assertEquals("1º Semestre", periods.get(0).getName());
        assertEquals("2º Semestre", periods.get(1).getName());
        assertEquals(type, periods.get(0).getPeriodType());

        // Segunda tentativa deve lançar IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                academicPeriodService.createDivision(testYear.getId(), type));
        assertTrue(ex.getMessage().contains("já possui uma divisão"));
    }

    @Test
    @DisplayName("ClassRoomService.create exige divisão prévia e update bloqueia alteração de periodType")
    void testClassRoomCreateAndUpdateValidation() {
        AcademicYear testYear = academicYearRepository.save(
                AcademicYear.builder()
                        .year(2095)
                        .startDate(LocalDate.of(2095, 2, 1))
                        .endDate(LocalDate.of(2095, 12, 15))
                        .minimumGrade(BigDecimal.valueOf(6.0))
                        .minimumAttendance(BigDecimal.valueOf(75.0))
                        .active(false)
                        .build()
        );

        ClassRoom cr = new ClassRoom();
        cr.setName("Turma Bimestre");
        cr.setShift(Shift.MORNING);
        cr.setSchoolGrade(SchoolGrade.FUNDAMENTAL_1);
        cr.setAcademicYear(testYear);
        cr.setPeriodType(AcademicPeriodType.BIMESTER);
        cr.setActive(true);

        // Deve falhar pois divisão ainda não foi criada
        IllegalStateException exCreate = assertThrows(IllegalStateException.class, () -> classRoomService.create(cr));
        assertTrue(exCreate.getMessage().contains("ainda não criou a divisão"));

        // Agora criamos a divisão
        academicPeriodService.createDivision(testYear.getId(), AcademicPeriodType.BIMESTER);

        // Agora a criação da turma deve ter sucesso
        ClassRoom saved = classRoomService.create(cr);
        assertNotNull(saved.getId());
        assertEquals(AcademicPeriodType.BIMESTER, saved.getPeriodType());

        // Tentativa de update alterando o periodType deve falhar
        ClassRoom incoming = new ClassRoom();
        incoming.setName("Turma Alterada");
        incoming.setShift(Shift.AFTERNOON);
        incoming.setSchoolGrade(SchoolGrade.FUNDAMENTAL_1);
        incoming.setAcademicYear(testYear);
        incoming.setPeriodType(AcademicPeriodType.TRIMESTER); // tipo diferente!

        IllegalStateException exUpdate = assertThrows(IllegalStateException.class, () ->
                classRoomService.update(saved.getId(), incoming));
        assertTrue(exUpdate.getMessage().contains("não pode ser alterado"));
    }

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    private TeacherClassSubject ensureActiveTrimesterTcs(AcademicYear year) {
        if (!academicPeriodRepository.existsByAcademicYearAndPeriodType(year, AcademicPeriodType.TRIMESTER)) {
            academicPeriodService.createDivision(year.getId(), AcademicPeriodType.TRIMESTER);
        }
        TeacherClassSubject existing = teacherClassSubjectRepository.findAll().stream()
                .filter(t -> t.getClassRoom().getAcademicYear() != null
                        && t.getClassRoom().getAcademicYear().getId().equals(year.getId())
                        && t.getClassRoom().getPeriodType() == AcademicPeriodType.TRIMESTER
                        && t.isActive())
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        ClassRoom cr = ClassRoom.builder()
                .name("Turma Tri Teste")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(year)
                .periodType(AcademicPeriodType.TRIMESTER)
                .active(true)
                .build();
        cr = classRoomRepository.save(cr);

        Subject subject = subjectRepository.findAll().stream().findFirst().orElseThrow();
        Teacher teacher = teacherRepository.findAll().stream().findFirst().orElseThrow();

        TeacherClassSubject tcs = TeacherClassSubject.builder()
                .classRoom(cr)
                .subject(subject)
                .teacher(teacher)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build();
        return teacherClassSubjectRepository.save(tcs);
    }

    @Test
    @DisplayName("Assessment.create bloqueia período com periodType diferente da turma")
    void testAssessmentPeriodTypeMismatch() {
        AcademicYear year = academicYearRepository.findByActiveTrue().orElseThrow();

        // Cria divisão SEMESTER se não existir
        if (!academicPeriodRepository.existsByAcademicYearAndPeriodType(year, AcademicPeriodType.SEMESTER)) {
            academicPeriodService.createDivision(year.getId(), AcademicPeriodType.SEMESTER);
        }
        List<AcademicPeriod> semesterPeriods = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.SEMESTER);

        TeacherClassSubject tcsTrimester = ensureActiveTrimesterTcs(year);
        assertNotNull(tcsTrimester, "Deve existir ao menos uma TCS para turma TRIMESTER no ano ativo");

        Assessment assessment = new Assessment();
        assessment.setTeacherClassSubject(tcsTrimester);
        assessment.setPeriod(semesterPeriods.get(0)); // período de semestre na turma de trimestre!
        assessment.setTitle("Avaliação Incompatível");
        assessment.setAssessmentType(AssessmentType.EXAM);
        assessment.setDate(LocalDate.now());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                assessmentService.create(assessment));
        assertTrue(ex.getMessage().contains("pertence a outra divisão"));
    }

    @Test
    @DisplayName("deleteDivision é bloqueada se houver turmas usando a divisão")
    void testDeleteDivisionBlockedByClassrooms() {
        AcademicYear year = academicYearRepository.findByActiveTrue().orElseThrow();
        ensureActiveTrimesterTcs(year);

        assertTrue(classRoomRepository.existsByAcademicYearAndPeriodTypeAndActiveTrue(year, AcademicPeriodType.TRIMESTER));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                academicPeriodService.deleteDivision(year.getId(), AcademicPeriodType.TRIMESTER));
        assertTrue(ex.getMessage().contains("existem turmas usando esta divisão"));
    }

    @Test
    @DisplayName("canConcludeYear valida apenas divisões em uso por turmas ativas")
    void testCanConcludeYearOnlyChecksDivisionsInUse() {
        AcademicYear year = academicYearRepository.findByActiveTrue().orElseThrow();
        ensureActiveTrimesterTcs(year);

        // Garante ao menos um período trimestral aberto
        List<AcademicPeriod> trimesterPeriods = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, AcademicPeriodType.TRIMESTER);
        trimesterPeriods.get(0).setClosed(false);
        academicPeriodRepository.save(trimesterPeriods.get(0));

        List<AcademicPeriodType> inUse = classRoomRepository.findDistinctActivePeriodTypesByYear(year);
        assertFalse(inUse.isEmpty());
        assertTrue(inUse.contains(AcademicPeriodType.TRIMESTER));

        // canConcludeYear deve ser falso porque os trimestres não estão todos fechados
        boolean canConclude = academicYearConclusionService.canConcludeYear();
        assertFalse(canConclude);
    }

    @Test
    @DisplayName("AcademicPeriodType define os divisores e quantidades corretas")
    void testAcademicPeriodTypeCountsAndDivisors() {
        assertEquals(4, AcademicPeriodType.BIMESTER.getPeriodsCount());
        assertEquals(4, AcademicPeriodType.BIMESTER.getDivisor());

        assertEquals(3, AcademicPeriodType.TRIMESTER.getPeriodsCount());
        assertEquals(3, AcademicPeriodType.TRIMESTER.getDivisor());

        assertEquals(2, AcademicPeriodType.SEMESTER.getPeriodsCount());
        assertEquals(2, AcademicPeriodType.SEMESTER.getDivisor());
    }

    @Test
    @DisplayName("SchoolSettingsService funciona sem campo periodType")
    void testSchoolSettingsWithoutPeriodType() {
        SchoolSettings settings = schoolSettingsService.findSettings();
        assertNotNull(settings);

        SchoolSettings toUpdate = SchoolSettings.builder()
                .schoolName("Escola Teste")
                .address("Rua Teste, 123")
                .phone("11999999999")
                .email("escola@teste.com")
                .minimumGrade(BigDecimal.valueOf(7.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .build();

        SchoolSettings res = schoolSettingsService.update(toUpdate);
        assertEquals("Escola Teste", res.getSchoolName());
        assertEquals(BigDecimal.valueOf(7.0), res.getMinimumGrade());
    }

    @Test
    @DisplayName("ClassRoomService.update permite omitir periodType e preserva a divisão original")
    void testClassRoomUpdateOmittingPeriodType() {
        AcademicYear year = academicYearRepository.findByActiveTrue().orElseThrow();

        if (!academicPeriodRepository.existsByAcademicYearAndPeriodType(year, AcademicPeriodType.SEMESTER)) {
            academicPeriodService.createDivision(year.getId(), AcademicPeriodType.SEMESTER);
        }

        ClassRoom cr = new ClassRoom();
        cr.setName("Turma Semestre Teste");
        cr.setShift(Shift.MORNING);
        cr.setSchoolGrade(SchoolGrade.FUNDAMENTAL_5);
        cr.setAcademicYear(year);
        cr.setPeriodType(AcademicPeriodType.SEMESTER);
        cr.setActive(true);
        ClassRoom saved = classRoomService.create(cr);

        // Update sem enviar periodType (null)
        ClassRoom incoming = new ClassRoom();
        incoming.setName("Turma Semestre Renomeada");
        incoming.setShift(Shift.AFTERNOON);
        incoming.setSchoolGrade(SchoolGrade.FUNDAMENTAL_6);
        incoming.setAcademicYear(year);
        incoming.setPeriodType(null); // omitido

        ClassRoom updated = classRoomService.update(saved.getId(), incoming);
        assertEquals("Turma Semestre Renomeada", updated.getName());
        assertEquals(Shift.AFTERNOON, updated.getShift());
        assertEquals(SchoolGrade.FUNDAMENTAL_6, updated.getSchoolGrade());
        assertEquals(AcademicPeriodType.SEMESTER, updated.getPeriodType(), "PeriodType original deve ser preservado");

        // Tentativa de criar ou atualizar turma sem schoolGrade deve lançar IllegalArgumentException
        ClassRoom noGrade = new ClassRoom();
        noGrade.setName("Turma Sem Serie");
        noGrade.setShift(Shift.MORNING);
        noGrade.setSchoolGrade(null);
        noGrade.setAcademicYear(year);
        noGrade.setPeriodType(AcademicPeriodType.SEMESTER);
        assertThrows(IllegalArgumentException.class, () -> classRoomService.create(noGrade));
        assertThrows(IllegalArgumentException.class, () -> classRoomService.update(saved.getId(), noGrade));
    }

    @Test
    @DisplayName("deleteDivision exclui divisão com sucesso se não houver turmas nem avaliações")
    void testDeleteDivisionSuccessWhenUnused() {
        AcademicYear testYear = academicYearRepository.save(
                AcademicYear.builder()
                        .year(2097)
                        .startDate(LocalDate.of(2097, 2, 1))
                        .endDate(LocalDate.of(2097, 12, 15))
                        .minimumGrade(BigDecimal.valueOf(6.0))
                        .minimumAttendance(BigDecimal.valueOf(75.0))
                        .active(false)
                        .build()
        );

        academicPeriodService.createDivision(testYear.getId(), AcademicPeriodType.BIMESTER);
        assertTrue(academicPeriodRepository.existsByAcademicYearAndPeriodType(testYear, AcademicPeriodType.BIMESTER));

        // Exclui divisão
        academicPeriodService.deleteDivision(testYear.getId(), AcademicPeriodType.BIMESTER);
        assertFalse(academicPeriodRepository.existsByAcademicYearAndPeriodType(testYear, AcademicPeriodType.BIMESTER));
    }

    @Test
    @DisplayName("openNextPeriod avança períodos da divisão e bloqueia quando chamado no último período")
    void testOpenNextPeriodAdvancesAndBlocksOnLast() {
        AcademicYear testYear = academicYearRepository.save(
                AcademicYear.builder()
                        .year(2098)
                        .startDate(LocalDate.of(2098, 2, 1))
                        .endDate(LocalDate.of(2098, 12, 15))
                        .minimumGrade(BigDecimal.valueOf(6.0))
                        .minimumAttendance(BigDecimal.valueOf(75.0))
                        .active(false)
                        .build()
        );

        List<AcademicPeriod> semPeriods = academicPeriodService.createDivision(testYear.getId(), AcademicPeriodType.SEMESTER);
        assertEquals(2, semPeriods.size());

        AcademicPeriod p1 = semPeriods.get(0);
        AcademicPeriod p2 = semPeriods.get(1);

        // Configura datas passadas para p1 poder ser fechado
        p1.setStartDate(LocalDate.now().minusMonths(6));
        p1.setEndDate(LocalDate.now().minusDays(1));
        academicPeriodRepository.save(p1);

        p2.setStartDate(LocalDate.now());
        p2.setEndDate(LocalDate.now().plusMonths(5));
        academicPeriodRepository.save(p2);

        // Avança de p1 para p2
        AcademicPeriod next = academicPeriodService.openNextPeriod(testYear.getId(), AcademicPeriodType.SEMESTER);
        assertEquals(p2.getId(), next.getId());
        assertTrue(academicPeriodRepository.findById(p1.getId()).orElseThrow().isClosed(), "P1 deve ter sido fechado");

        // Tentar avançar além de p2 (que é o último) deve lançar erro informativo sem fechar p2
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                academicPeriodService.openNextPeriod(testYear.getId(), AcademicPeriodType.SEMESTER));
        assertTrue(ex.getMessage().contains("último da divisão"), "Deve indicar que é o último período da divisão");
        assertFalse(academicPeriodRepository.findById(p2.getId()).orElseThrow().isClosed(), "P2 não deve ter sido fechado pelo erro");
    }

    @Test
    @DisplayName("Conclusão do ano é permitida quando todos os períodos das turmas ativas estão fechados, mesmo que divisão não usada esteja aberta")
    void testConclusionAllowedWhenUnusedDivisionIsOpen() {
        AcademicYear currentActive = academicYearRepository.findByActiveTrue().orElseThrow();
        currentActive.setActive(false);
        academicYearRepository.save(currentActive);

        AcademicYear testYear = academicYearRepository.save(
                AcademicYear.builder()
                        .year(2096)
                        .startDate(LocalDate.of(2096, 2, 1))
                        .endDate(LocalDate.of(2096, 12, 15))
                        .minimumGrade(BigDecimal.valueOf(6.0))
                        .minimumAttendance(BigDecimal.valueOf(75.0))
                        .active(true)
                        .build()
        );

        // Cria divisão TRIMESTER e turma ativa para testYear
        academicPeriodService.createDivision(testYear.getId(), AcademicPeriodType.TRIMESTER);
        classRoomRepository.save(ClassRoom.builder()
                .name("Turma Isolada 2096")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.MEDIO_1)
                .academicYear(testYear)
                .periodType(AcademicPeriodType.TRIMESTER)
                .active(true)
                .build());

        // Fecha todos os períodos de TRIMESTER (a única divisão em uso)
        List<AcademicPeriod> periods = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(testYear, AcademicPeriodType.TRIMESTER);
        for (AcademicPeriod p : periods) {
            p.setClosed(true);
            academicPeriodRepository.save(p);
        }

        // Cria divisão SEMESTER (não usada por turmas ativas, com períodos abertos)
        academicPeriodService.createDivision(testYear.getId(), AcademicPeriodType.SEMESTER);

        // canConcludeYear deve ser true porque TRIMESTER está 100% fechada e SEMESTER não tem turmas ativas
        assertTrue(academicYearConclusionService.canConcludeYear());
    }

    @Test
    @DisplayName("ClassRoom.update ao trocar ano letivo bloqueia se novo ano não possuir a divisão da turma")
    void testClassRoomUpdateAcademicYearWithoutDivisionThrowsException() {
        AcademicYear year1 = academicYearRepository.save(AcademicYear.builder()
                .year(2080)
                .startDate(LocalDate.of(2080, 2, 1))
                .endDate(LocalDate.of(2080, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        academicPeriodService.createDivision(year1.getId(), AcademicPeriodType.TRIMESTER);

        ClassRoom cr = classRoomService.create(ClassRoom.builder()
                .name("Turma Troca Sem Div")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_1)
                .academicYear(year1)
                .periodType(AcademicPeriodType.TRIMESTER)
                .active(true)
                .build());

        AcademicYear year2 = academicYearRepository.save(AcademicYear.builder()
                .year(2081)
                .startDate(LocalDate.of(2081, 2, 1))
                .endDate(LocalDate.of(2081, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        // year2 NÃO tem a divisão TRIMESTER

        ClassRoom incoming = new ClassRoom();
        incoming.setName("Turma Troca Sem Div");
        incoming.setShift(Shift.MORNING);
        incoming.setSchoolGrade(SchoolGrade.FUNDAMENTAL_1);
        incoming.setAcademicYear(year2);
        incoming.setPeriodType(AcademicPeriodType.TRIMESTER);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                classRoomService.update(cr.getId(), incoming));
        assertTrue(ex.getMessage().contains("ainda não criou a divisão"));
    }

    @Test
    @DisplayName("ClassRoom.update ao trocar ano letivo bloqueia se turma possuir matrículas vinculadas")
    void testClassRoomUpdateAcademicYearWithEnrollmentsThrowsException() {
        AcademicYear year1 = academicYearRepository.save(AcademicYear.builder()
                .year(2082)
                .startDate(LocalDate.of(2082, 2, 1))
                .endDate(LocalDate.of(2082, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        academicPeriodService.createDivision(year1.getId(), AcademicPeriodType.TRIMESTER);

        ClassRoom cr = classRoomService.create(ClassRoom.builder()
                .name("Turma Com Matrículas")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_2)
                .academicYear(year1)
                .periodType(AcademicPeriodType.TRIMESTER)
                .active(true)
                .build());

        AcademicYear year2 = academicYearRepository.save(AcademicYear.builder()
                .year(2083)
                .startDate(LocalDate.of(2083, 2, 1))
                .endDate(LocalDate.of(2083, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        academicPeriodService.createDivision(year2.getId(), AcademicPeriodType.TRIMESTER);

        Student student = studentRepository.save(Student.builder()
                .name("Aluno Teste Troca Ano")
                .email("aluno.trocaano@escola.com")
                .password("senha12345")
                .registrationNumber("MAT-ANO-01")
                .birthDate(LocalDate.of(2010, 1, 1))
                .active(true)
                .build());

        enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .classRoom(cr)
                .active(true)
                .build());

        ClassRoom incoming = new ClassRoom();
        incoming.setName("Turma Com Matrículas");
        incoming.setShift(Shift.MORNING);
        incoming.setSchoolGrade(SchoolGrade.FUNDAMENTAL_2);
        incoming.setAcademicYear(year2);
        incoming.setPeriodType(AcademicPeriodType.TRIMESTER);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                classRoomService.update(cr.getId(), incoming));
        assertEquals("Não é possível alterar o ano letivo da turma '" + cr.getName() + "' pois ela já possui matrículas vinculadas.", ex.getMessage());
    }

    @Test
    @DisplayName("ClassRoom.update ao trocar ano letivo bloqueia se turma possuir disciplinas/professores vinculados")
    void testClassRoomUpdateAcademicYearWithTeacherAssignmentsThrowsException() {
        AcademicYear year1 = academicYearRepository.save(AcademicYear.builder()
                .year(2084)
                .startDate(LocalDate.of(2084, 2, 1))
                .endDate(LocalDate.of(2084, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        academicPeriodService.createDivision(year1.getId(), AcademicPeriodType.TRIMESTER);

        ClassRoom cr = classRoomService.create(ClassRoom.builder()
                .name("Turma Com Discip")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_3)
                .academicYear(year1)
                .periodType(AcademicPeriodType.TRIMESTER)
                .active(true)
                .build());

        AcademicYear year2 = academicYearRepository.save(AcademicYear.builder()
                .year(2085)
                .startDate(LocalDate.of(2085, 2, 1))
                .endDate(LocalDate.of(2085, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        academicPeriodService.createDivision(year2.getId(), AcademicPeriodType.TRIMESTER);

        Subject subject = subjectRepository.findAll().stream().findFirst().orElseThrow();
        Teacher teacher = teacherRepository.findAll().stream().findFirst().orElseThrow();

        teacherClassSubjectRepository.save(TeacherClassSubject.builder()
                .classRoom(cr)
                .subject(subject)
                .teacher(teacher)
                .minAssessmentsPerPeriod(1)
                .maxAssessmentsPerPeriod(3)
                .active(true)
                .build());

        ClassRoom incoming = new ClassRoom();
        incoming.setName("Turma Com Discip");
        incoming.setShift(Shift.MORNING);
        incoming.setSchoolGrade(SchoolGrade.FUNDAMENTAL_3);
        incoming.setAcademicYear(year2);
        incoming.setPeriodType(AcademicPeriodType.TRIMESTER);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                classRoomService.update(cr.getId(), incoming));
        assertEquals("Não é possível alterar o ano letivo da turma '" + cr.getName() + "' pois ela já possui disciplinas ou professores vinculados.", ex.getMessage());
    }

    @Test
    @DisplayName("ClassRoom.update ao trocar ano letivo tem sucesso se turma não possuir matrículas nem atribuições")
    void testClassRoomUpdateAcademicYearCleanSucceeds() {
        AcademicYear year1 = academicYearRepository.save(AcademicYear.builder()
                .year(2086)
                .startDate(LocalDate.of(2086, 2, 1))
                .endDate(LocalDate.of(2086, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        academicPeriodService.createDivision(year1.getId(), AcademicPeriodType.TRIMESTER);

        ClassRoom cr = classRoomService.create(ClassRoom.builder()
                .name("Turma Limpa Troca")
                .shift(Shift.MORNING)
                .schoolGrade(SchoolGrade.FUNDAMENTAL_4)
                .academicYear(year1)
                .periodType(AcademicPeriodType.TRIMESTER)
                .active(true)
                .build());

        AcademicYear year2 = academicYearRepository.save(AcademicYear.builder()
                .year(2087)
                .startDate(LocalDate.of(2087, 2, 1))
                .endDate(LocalDate.of(2087, 12, 15))
                .minimumGrade(BigDecimal.valueOf(6.0))
                .minimumAttendance(BigDecimal.valueOf(75.0))
                .active(false)
                .build());
        academicPeriodService.createDivision(year2.getId(), AcademicPeriodType.TRIMESTER);

        ClassRoom incoming = new ClassRoom();
        incoming.setName("Turma Limpa Troca");
        incoming.setShift(Shift.MORNING);
        incoming.setSchoolGrade(SchoolGrade.FUNDAMENTAL_4);
        incoming.setAcademicYear(year2);
        incoming.setPeriodType(AcademicPeriodType.TRIMESTER);

        ClassRoom updated = classRoomService.update(cr.getId(), incoming);
        assertEquals(year2.getId(), updated.getAcademicYear().getId());
    }
}
