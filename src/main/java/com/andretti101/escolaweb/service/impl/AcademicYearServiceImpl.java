package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.service.AcademicYearService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Gerenciamento do ciclo de vida dos anos letivos, ativação de ano corrente e garantia de unicidade de ano ativo
@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final ClassRoomRepository classRoomRepository;
    private final AcademicPeriodRepository academicPeriodRepository;

    // Cadastra um novo ano letivo garantindo que não haja duplicidade de ano civil
    @Override
    @Transactional
    public AcademicYear create(AcademicYear academicYear) {
        if (academicYearRepository.existsByYear(academicYear.getYear())) {
            throw new IllegalStateException("Já existe um ano letivo cadastrado para o ano " + academicYear.getYear() + ".");
        }
        academicYear.setActive(false);
        return academicYearRepository.save(academicYear);
    }

    @Override
    @Transactional
    public AcademicYear update(Integer id, AcademicYear incoming) {
        AcademicYear existing = findYearOrThrow(id);

        if (!existing.getYear().equals(incoming.getYear())
                && academicYearRepository.existsByYearAndIdNot(incoming.getYear(), id)) {
            throw new IllegalStateException("Já existe um ano letivo cadastrado para o ano " + incoming.getYear() + ".");
        }

        existing.setYear(incoming.getYear());
        existing.setStartDate(incoming.getStartDate());
        existing.setEndDate(incoming.getEndDate());

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        AcademicYear year = findYearOrThrow(id);

        if (classRoomRepository.existsByAcademicYear(year)) {
            throw new IllegalStateException(
                    "Não é possível excluir o ano letivo " + year.getYear() + " pois ele possui turmas vinculadas. Remova as turmas antes de excluir.");
        }
        if (academicPeriodRepository.existsByAcademicYear(year)) {
            throw new IllegalStateException(
                    "Não é possível excluir o ano letivo " + year.getYear() + " pois ele possui períodos vinculados. Remova os períodos antes de excluir.");
        }

        academicYearRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYear findById(Integer id) {
        return findYearOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYear> findAll() {
        return academicYearRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AcademicYear> findAll(org.springframework.data.domain.Pageable pageable) {
        return academicYearRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYear findActive() {
        return academicYearRepository.findByActiveTrue()
                .orElseThrow(() -> new EntityNotFoundException("Nenhum ano letivo ativo foi encontrado."));
    }

    @Override
    @Transactional
    public AcademicYear setActive(Integer id) {
        AcademicYear target = findYearOrThrow(id);

        academicYearRepository.findByActiveTrue().ifPresent(current -> current.setActive(false));

        target.setActive(true);
        return target;
    }

    AcademicYear findYearOrThrow(Integer id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ano letivo não encontrado."));
    }
}
