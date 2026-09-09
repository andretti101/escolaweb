package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AssessmentRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AcademicYearService;
import com.andretti101.escolaweb.service.SchoolSettingsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicPeriodServiceImpl implements AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final AssessmentRepository assessmentRepository;
    private final AcademicYearService academicYearService;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final SchoolSettingsService schoolSettingsService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional
    public AcademicPeriod create(AcademicPeriod period) {
        validateAcademicYearExists(period.getAcademicYear());
        period.setClosed(false);
        return academicPeriodRepository.save(period);
    }

    @Override
    @Transactional
    public AcademicPeriod update(Integer id, AcademicPeriod incoming) {
        AcademicPeriod existing = findPeriodOrThrow(id);

        if (existing.isClosed()) {
            throw new IllegalStateException("Não é possível atualizar o período '" + existing.getName() + "' pois ele já está fechado.");
        }

        validateAcademicYearExists(incoming.getAcademicYear());

        // Validação de cronologia: garantir que as datas não se sobrepõem com períodos adjacentes
        if (incoming.getStartDate() != null || incoming.getEndDate() != null) {
            validateChronology(existing, incoming.getStartDate(), incoming.getEndDate());
        }

        existing.setName(incoming.getName());
        existing.setStartDate(incoming.getStartDate());
        existing.setEndDate(incoming.getEndDate());
        existing.setAcademicYear(incoming.getAcademicYear());

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        AcademicPeriod period = findPeriodOrThrow(id);

        if (period.isClosed()) {
            throw new IllegalStateException("Cannot delete a closed academic period with id: " + id);
        }
        if (assessmentRepository.existsByPeriod(period)) {
            throw new IllegalStateException(
                    "Cannot delete period with id " + id + " because it has assessments linked to it.");
        }

        academicPeriodRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicPeriod findById(Integer id) {
        return findPeriodOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicPeriod> findAll() {
        return academicPeriodRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicPeriod> findByAcademicYear(Integer academicYearId) {
        AcademicYear year = academicYearService.findById(academicYearId);
        return academicPeriodRepository.findByAcademicYear(year);
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRÉ-GERAÇÃO AUTOMÁTICA
    // ══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public List<AcademicPeriod> generatePeriodsForYear(Integer academicYearId) {
        AcademicYear year = academicYearService.findById(academicYearId);

        // Impedir duplicação
        List<AcademicPeriod> existing = academicPeriodRepository.findByAcademicYear(year);
        if (!existing.isEmpty()) {
            throw new IllegalStateException(
                    "O ano letivo " + year.getYear() + " já possui " + existing.size()
                    + " período(s) acadêmico(s) gerado(s). Remova-os antes de gerar novamente.");
        }

        AcademicPeriodType periodType = schoolSettingsService.findSettings().getPeriodType();
        int count = getPeriodsCount(periodType);
        String label = periodType.getLabel();

        List<AcademicPeriod> periods = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String name = count == 1 ? "Período Anual" : i + "º " + label;
            AcademicPeriod period = AcademicPeriod.builder()
                    .academicYear(year)
                    .name(name)
                    .closed(false)
                    .build();
            periods.add(academicPeriodRepository.save(period));
        }

        return periods;
    }

    // ══════════════════════════════════════════════════════════════════════
    // FECHAMENTO DE PERÍODO
    // ══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AcademicPeriod close(Integer id) {
        AcademicPeriod period = findPeriodOrThrow(id);

        if (period.isClosed()) {
            throw new IllegalStateException("O período '" + period.getName() + "' já está fechado.");
        }

        validateCanClose(period);

        period.setClosed(true);
        return period;
    }

    // ══════════════════════════════════════════════════════════════════════
    // TRANSIÇÃO: ABRIR PRÓXIMO PERÍODO
    // ══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AcademicPeriod openNextPeriod(Integer academicYearId) {
        AcademicYear year = academicYearService.findById(academicYearId);
        List<AcademicPeriod> allPeriods = academicPeriodRepository.findByAcademicYearOrderByIdAsc(year);

        if (allPeriods.isEmpty()) {
            throw new IllegalStateException(
                    "O ano letivo " + year.getYear() + " não possui períodos acadêmicos. Gere-os primeiro.");
        }

        // Encontrar o período atualmente ativo (não fechado + com datas = em andamento)
        AcademicPeriod currentOpen = allPeriods.stream()
                .filter(p -> !p.isClosed() && p.getStartDate() != null && p.getEndDate() != null)
                .findFirst()
                .orElse(null);

        if (currentOpen != null) {
            // Validar e fechar o período atual
            validateCanClose(currentOpen);
            currentOpen.setClosed(true);
        }

        // Encontrar o próximo período pendente (não fechado e diferente do atual)
        AcademicPeriod nextPeriod = allPeriods.stream()
                .filter(p -> !p.isClosed())
                .filter(p -> currentOpen == null || !p.getId().equals(currentOpen.getId()))
                .findFirst()
                .orElse(null);

        if (nextPeriod == null) {
            throw new IllegalStateException(
                    "Todos os períodos do ano letivo " + year.getYear()
                    + " já foram fechados. Não há mais períodos para abrir.");
        }

        // Validar cronologia: novo período não pode começar antes do anterior terminar
        if (currentOpen != null && nextPeriod.getStartDate() != null && currentOpen.getEndDate() != null) {
            if (nextPeriod.getStartDate().isBefore(currentOpen.getEndDate())) {
                throw new IllegalStateException(
                        "A data de início do período '" + nextPeriod.getName()
                        + "' (" + nextPeriod.getStartDate().format(DATE_FMT)
                        + ") é anterior à data de término do período '"
                        + currentOpen.getName() + "' (" + currentOpen.getEndDate().format(DATE_FMT)
                        + "). Ajuste as datas antes de prosseguir.");
            }
        }

        return nextPeriod;
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDADORES ISOLADOS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Valida se um período pode ser fechado.
     * Roda 3 regras: datas definidas, data de término atingida, mínimo de avaliações.
     */
    private void validateCanClose(AcademicPeriod period) {
        // Regra 1: Data de término deve estar definida
        if (period.getEndDate() == null) {
            throw new IllegalStateException(
                    "Não é possível fechar o período '" + period.getName()
                    + "': a data de término deve ser definida antes do fechamento.");
        }

        // Regra 2: Data atual deve ser >= data de término
        if (LocalDate.now().isBefore(period.getEndDate())) {
            throw new IllegalStateException(
                    "Não é possível fechar o período '" + period.getName()
                    + "': a data de término (" + period.getEndDate().format(DATE_FMT)
                    + ") ainda não foi atingida.");
        }

        // Regra 3: Mínimo de avaliações por matéria/turma
        validateMinimumAssessments(period);
    }

    /**
     * Valida que todas as matérias/turmas DO MESMO ANO LETIVO atingiram
     * o mínimo de avaliações obrigatórias para o período.
     */
    private void validateMinimumAssessments(AcademicPeriod period) {
        // Filtrar apenas TCS do mesmo ano letivo do período
        List<TeacherClassSubject> relevantTcs = teacherClassSubjectRepository.findAll().stream()
                .filter(tcs -> tcs.getClassRoom().getAcademicYear() != null
                        && tcs.getClassRoom().getAcademicYear().getId().equals(period.getAcademicYear().getId()))
                .toList();

        for (TeacherClassSubject tcs : relevantTcs) {
            if (tcs.getMinAssessmentsPerPeriod() != null) {
                long assessmentCount = assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, period);

                if (assessmentCount < tcs.getMinAssessmentsPerPeriod()) {
                    throw new IllegalStateException(
                            String.format("Não é possível fechar o período '%s': a matéria '%s' na turma '%s' possui apenas %d avaliação(ões), mas exige no mínimo %d.",
                                    period.getName(),
                                    tcs.getSubject().getName(),
                                    tcs.getClassRoom().getName(),
                                    assessmentCount,
                                    tcs.getMinAssessmentsPerPeriod())
                    );
                }
            }
        }
    }

    /**
     * Valida a cronologia das datas de um período em relação aos seus vizinhos.
     * Impede sobreposição de datas entre períodos adjacentes.
     */
    private void validateChronology(AcademicPeriod existing, LocalDate newStartDate, LocalDate newEndDate) {
        List<AcademicPeriod> allPeriods = academicPeriodRepository
                .findByAcademicYearOrderByIdAsc(existing.getAcademicYear());

        int currentIndex = -1;
        for (int i = 0; i < allPeriods.size(); i++) {
            if (allPeriods.get(i).getId().equals(existing.getId())) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) return;

        // Validar contra o período ANTERIOR
        if (currentIndex > 0) {
            AcademicPeriod previous = allPeriods.get(currentIndex - 1);
            if (previous.getEndDate() != null && newStartDate != null) {
                if (newStartDate.isBefore(previous.getEndDate())) {
                    throw new IllegalStateException(
                            "A data de início (" + newStartDate.format(DATE_FMT)
                            + ") do período '" + existing.getName()
                            + "' não pode ser anterior à data de término ("
                            + previous.getEndDate().format(DATE_FMT)
                            + ") do período anterior '" + previous.getName() + "'.");
                }
            }
        }

        // Validar contra o período POSTERIOR
        if (currentIndex < allPeriods.size() - 1) {
            AcademicPeriod next = allPeriods.get(currentIndex + 1);
            if (next.getStartDate() != null && newEndDate != null) {
                if (newEndDate.isAfter(next.getStartDate())) {
                    throw new IllegalStateException(
                            "A data de término (" + newEndDate.format(DATE_FMT)
                            + ") do período '" + existing.getName()
                            + "' não pode ser posterior à data de início ("
                            + next.getStartDate().format(DATE_FMT)
                            + ") do período seguinte '" + next.getName() + "'.");
                }
            }
        }

        // Validar que startDate < endDate
        if (newStartDate != null && newEndDate != null && !newStartDate.isBefore(newEndDate)) {
            throw new IllegalStateException(
                    "A data de início (" + newStartDate.format(DATE_FMT)
                    + ") deve ser anterior à data de término (" + newEndDate.format(DATE_FMT)
                    + ") do período '" + existing.getName() + "'.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPERS INTERNOS
    // ══════════════════════════════════════════════════════════════════════

    private int getPeriodsCount(AcademicPeriodType type) {
        if (type == null) return 3;
        return switch (type) {
            case BIMESTER -> 4;
            case TRIMESTER -> 3;
            case QUADRIMESTER -> 3;
            case SEMESTER -> 2;
            case ANNUAL -> 1;
        };
    }

    AcademicPeriod findPeriodOrThrow(Integer id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Período acadêmico não encontrado com id: " + id));
    }

    private void validateAcademicYearExists(AcademicYear academicYear) {
        if (academicYear == null || academicYear.getId() == null) {
            throw new IllegalArgumentException("O ano letivo é obrigatório.");
        }
        academicYearService.findById(academicYear.getId());
    }
}