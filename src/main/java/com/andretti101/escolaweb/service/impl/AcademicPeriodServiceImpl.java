package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.AcademicPeriodType;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AssessmentRepository;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AcademicYearService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AcademicPeriodServiceImpl implements AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final AssessmentRepository assessmentRepository;
    private final AcademicYearService academicYearService;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final ClassRoomRepository classRoomRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static final Comparator<AcademicPeriod> CHRONOLOGICAL_DESC = (p1, p2) -> {
        int closedCompare = Boolean.compare(p1.isClosed(), p2.isClosed());
        if (closedCompare != 0) {
            return closedCompare; // Open (false) before Closed (true)
        }
        if (p1.getStartDate() != null && p2.getStartDate() != null && !p1.getStartDate().isEqual(p2.getStartDate())) {
            return p2.getStartDate().compareTo(p1.getStartDate()); // StartDate DESC
        }
        if (p1.getStartDate() == null && p2.getStartDate() != null) return 1;
        if (p1.getStartDate() != null && p2.getStartDate() == null) return -1;

        int year1 = p1.getAcademicYear() != null && p1.getAcademicYear().getYear() != null ? p1.getAcademicYear().getYear() : 0;
        int year2 = p2.getAcademicYear() != null && p2.getAcademicYear().getYear() != null ? p2.getAcademicYear().getYear() : 0;
        if (year1 != year2) {
            return Integer.compare(year2, year1); // Year DESC
        }
        Integer id1 = p1.getId() != null ? p1.getId() : 0;
        Integer id2 = p2.getId() != null ? p2.getId() : 0;
        return Integer.compare(id2, id1); // ID DESC
    };

    @Override
    @Transactional
    public AcademicPeriod create(AcademicPeriod period) {
        if (period.getPeriodType() == null) {
            throw new IllegalArgumentException("O tipo de divisão do período é obrigatório.");
        }
        AcademicYear year = validateAcademicYearExists(period.getAcademicYear());
        period.setAcademicYear(year);

        List<AcademicPeriod> existing = academicPeriodRepository
                .findByAcademicYearAndPeriodTypeOrderByIdAsc(year, period.getPeriodType());
        if (existing.size() >= period.getPeriodType().getPeriodsCount()) {
            throw new IllegalStateException("O ano letivo " + year.getYear() + " já atingiu o limite de "
                    + period.getPeriodType().getPeriodsCount() + " período(s) para a divisão "
                    + period.getPeriodType().getLabel() + ".");
        }

        period.setClosed(false);
        return academicPeriodRepository.save(period);
    }

    @Override
    @Transactional
    public List<AcademicPeriod> createDivision(Integer academicYearId, AcademicPeriodType type) {
        if (type == null) {
            throw new IllegalArgumentException("O tipo de divisão é obrigatório.");
        }
        AcademicYear year = academicYearService.findById(academicYearId);

        if (academicPeriodRepository.existsByAcademicYearAndPeriodType(year, type)) {
            throw new IllegalStateException("O ano letivo " + year.getYear()
                    + " já possui uma divisão do tipo " + type.getLabel() + ".");
        }

        int count = type.getPeriodsCount();
        String label = type.getLabel();
        List<AcademicPeriod> periods = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String name = i + "º " + label;
            AcademicPeriod period = AcademicPeriod.builder()
                    .academicYear(year)
                    .periodType(type)
                    .name(name)
                    .closed(false)
                    .build();
            periods.add(academicPeriodRepository.save(period));
        }

        return periods;
    }

    @Override
    @Transactional
    public AcademicPeriod update(Integer id, AcademicPeriod incoming) {
        AcademicPeriod existing = findPeriodOrThrow(id);

        if (existing.isClosed()) {
            throw new IllegalStateException("Não é possível atualizar o período '" + existing.getName() + "' pois ele já está fechado.");
        }

        if (incoming.getPeriodType() != null && incoming.getPeriodType() != existing.getPeriodType()) {
            throw new IllegalStateException("O tipo de divisão do período não pode ser alterado.");
        }
        if (incoming.getAcademicYear() != null && incoming.getAcademicYear().getId() != null
                && !incoming.getAcademicYear().getId().equals(existing.getAcademicYear().getId())) {
            throw new IllegalStateException("O ano letivo do período não pode ser alterado.");
        }

        // Validação de cronologia: garantir que as datas não se sobrepõem com períodos adjacentes da mesma divisão
        if (incoming.getStartDate() != null || incoming.getEndDate() != null) {
            validateChronology(existing, incoming.getStartDate(), incoming.getEndDate());
        }

        existing.setName(incoming.getName());
        existing.setStartDate(incoming.getStartDate());
        existing.setEndDate(incoming.getEndDate());

        return academicPeriodRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        AcademicPeriod period = findPeriodOrThrow(id);

        if (period.isClosed()) {
            throw new IllegalStateException("Não é possível excluir o período '" + period.getName() + "' pois ele já está fechado.");
        }
        if (assessmentRepository.existsByPeriod(period)) {
            throw new IllegalStateException(
                    "Não é possível excluir o período '" + period.getName() + "' pois ele possui avaliações vinculadas. Remova as avaliações antes de excluir.");
        }
        if (classRoomRepository.existsByAcademicYearAndPeriodTypeAndActiveTrue(period.getAcademicYear(), period.getPeriodType())) {
            throw new IllegalStateException("Não é possível excluir: existem turmas usando esta divisão.");
        }

        academicPeriodRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteDivision(Integer academicYearId, AcademicPeriodType type) {
        AcademicYear year = academicYearService.findById(academicYearId);

        if (classRoomRepository.existsByAcademicYearAndPeriodTypeAndActiveTrue(year, type)) {
            throw new IllegalStateException("Não é possível excluir: existem turmas usando esta divisão.");
        }

        List<AcademicPeriod> periods = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, type);
        if (periods.isEmpty()) {
            throw new EntityNotFoundException("Nenhum período encontrado para a divisão " + type.getLabel() + " no ano letivo " + year.getYear() + ".");
        }
        for (AcademicPeriod p : periods) {
            if (p.isClosed()) {
                throw new IllegalStateException("Não é possível excluir a divisão pois o período '" + p.getName() + "' já está fechado.");
            }
            if (assessmentRepository.existsByPeriod(p)) {
                throw new IllegalStateException("Não é possível excluir a divisão pois o período '" + p.getName() + "' possui avaliações vinculadas.");
            }
        }

        academicPeriodRepository.deleteAll(periods);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicPeriod findById(Integer id) {
        return findPeriodOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicPeriod> findAll() {
        List<AcademicPeriod> list = new ArrayList<>(academicPeriodRepository.findAllByOrderByAcademicYearYearDescIdDesc());
        list.sort(CHRONOLOGICAL_DESC);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AcademicPeriod> findAll(org.springframework.data.domain.Pageable pageable) {
        return academicPeriodRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicPeriod> findByAcademicYear(Integer academicYearId) {
        AcademicYear year = academicYearService.findById(academicYearId);
        List<AcademicPeriod> list = new ArrayList<>(academicPeriodRepository.findByAcademicYearOrderByIdDesc(year));
        list.sort(CHRONOLOGICAL_DESC);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicPeriod> findByAcademicYearAndType(Integer academicYearId, AcademicPeriodType type) {
        AcademicYear year = academicYearService.findById(academicYearId);
        List<AcademicPeriod> list = new ArrayList<>(academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, type));
        list.sort(CHRONOLOGICAL_DESC);
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<AcademicPeriodType, List<AcademicPeriod>> findDivisions(Integer academicYearId) {
        AcademicYear year = academicYearService.findById(academicYearId);
        List<AcademicPeriod> allPeriods = academicPeriodRepository.findByAcademicYearOrderByIdAsc(year);

        Map<AcademicPeriodType, List<AcademicPeriod>> map = new LinkedHashMap<>();
        for (AcademicPeriod p : allPeriods) {
            if (p.getPeriodType() != null) {
                map.computeIfAbsent(p.getPeriodType(), k -> new ArrayList<>()).add(p);
            }
        }
        return map;
    }

    @Override
    @Transactional
    public AcademicPeriod close(Integer id) {
        AcademicPeriod period = findPeriodOrThrow(id);

        if (period.isClosed()) {
            throw new IllegalStateException("O período '" + period.getName() + "' já está fechado.");
        }

        validateCanClose(period);

        period.setClosed(true);
        return academicPeriodRepository.save(period);
    }

    @Override
    @Transactional
    public AcademicPeriod openNextPeriod(Integer academicYearId, AcademicPeriodType type) {
        if (type == null) {
            throw new IllegalArgumentException("O tipo de divisão é obrigatório para abrir o próximo período.");
        }
        AcademicYear year = academicYearService.findById(academicYearId);
        List<AcademicPeriod> allPeriods = academicPeriodRepository.findByAcademicYearAndPeriodTypeOrderByIdAsc(year, type);
        if (allPeriods.isEmpty()) {
            throw new IllegalStateException(
                    "O ano letivo " + year.getYear() + " não possui períodos da divisão " + type.getLabel() + ".");
        }

        AcademicPeriod currentOpen = allPeriods.stream()
                .filter(p -> !p.isClosed())
                .findFirst()
                .orElse(null);

        if (currentOpen == null) {
            throw new IllegalStateException("Todos os períodos da divisão " + type.getLabel() + " já foram fechados. Não há mais períodos para abrir.");
        }

        int currentIndex = allPeriods.indexOf(currentOpen);
        if (currentIndex < 0 || currentIndex >= allPeriods.size() - 1) {
            throw new IllegalStateException("O período '" + currentOpen.getName() + "' é o último da divisão " + type.getLabel() + ". Para encerrar o ciclo, feche o período diretamente.");
        }

        AcademicPeriod nextPeriod = allPeriods.get(currentIndex + 1);

        validateCanClose(currentOpen);

        if (nextPeriod.getStartDate() != null && currentOpen.getEndDate() != null) {
            if (nextPeriod.getStartDate().isBefore(currentOpen.getEndDate())) {
                throw new IllegalStateException(
                        "A data de início do período '" + nextPeriod.getName()
                        + "' (" + nextPeriod.getStartDate().format(DATE_FMT)
                        + ") é anterior à data de término do período '"
                        + currentOpen.getName() + "' (" + currentOpen.getEndDate().format(DATE_FMT)
                        + "). Ajuste as datas antes de prosseguir.");
            }
        }

        currentOpen.setClosed(true);
        academicPeriodRepository.save(currentOpen);

        return nextPeriod;
    }

    private void validateCanClose(AcademicPeriod period) {
        if (period.getEndDate() == null) {
            throw new IllegalStateException(
                    "Não é possível fechar o período '" + period.getName()
                    + "': a data de término deve ser definida antes do fechamento.");
        }

        if (LocalDate.now().isBefore(period.getEndDate())) {
            throw new IllegalStateException(
                    "Não é possível fechar o período '" + period.getName()
                    + "': a data de término (" + period.getEndDate().format(DATE_FMT)
                    + ") ainda não foi atingida.");
        }

        validateMinimumAssessments(period);
    }

    private void validateMinimumAssessments(AcademicPeriod period) {
        List<TeacherClassSubject> relevantTcs = teacherClassSubjectRepository
                .findActiveByYearAndPeriodType(period.getAcademicYear().getId(), period.getPeriodType());

        if (relevantTcs == null || relevantTcs.isEmpty()) {
            relevantTcs = teacherClassSubjectRepository.findAll().stream()
                    .filter(tcs -> tcs.isActive()
                            && tcs.getClassRoom() != null
                            && tcs.getClassRoom().isActive()
                            && tcs.getClassRoom().getAcademicYear() != null
                            && tcs.getClassRoom().getAcademicYear().getId().equals(period.getAcademicYear().getId())
                            && tcs.getClassRoom().getPeriodType() == period.getPeriodType())
                    .toList();
        }

        for (TeacherClassSubject tcs : relevantTcs) {
            if (tcs.getMinAssessmentsPerPeriod() != null) {
                long assessmentCount = assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, period);

                if (assessmentCount < tcs.getMinAssessmentsPerPeriod()) {
                    throw new IllegalStateException(
                            String.format("Não é possível fechar o período '%s': a matéria '%s' na turma '%s' possui apenas %d avaliação(ões), mas exige no mínimo %d. Lance as avaliações pendentes antes de fechar o período.",
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

    private void validateChronology(AcademicPeriod existing, LocalDate newStartDate, LocalDate newEndDate) {
        List<AcademicPeriod> allPeriods = academicPeriodRepository
                .findByAcademicYearAndPeriodTypeOrderByIdAsc(existing.getAcademicYear(), existing.getPeriodType());

        int currentIndex = -1;
        for (int i = 0; i < allPeriods.size(); i++) {
            if (allPeriods.get(i).getId().equals(existing.getId())) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) return;

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

        if (newStartDate != null && newEndDate != null && !newStartDate.isBefore(newEndDate)) {
            throw new IllegalStateException(
                    "A data de início (" + newStartDate.format(DATE_FMT)
                    + ") deve ser anterior à data de término (" + newEndDate.format(DATE_FMT)
                    + ") do período '" + existing.getName() + "'. Verifique o intervalo de datas informado.");
        }
    }

    AcademicPeriod findPeriodOrThrow(Integer id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Período acadêmico não encontrado."));
    }

    private AcademicYear validateAcademicYearExists(AcademicYear academicYear) {
        if (academicYear == null || academicYear.getId() == null) {
            throw new IllegalArgumentException("A seleção do ano letivo é obrigatória.");
        }
        return academicYearService.findById(academicYear.getId());
    }
}