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

@Service
@RequiredArgsConstructor
public class AcademicYearServiceImpl implements AcademicYearService {

    private final AcademicYearRepository academicYearRepository;
    private final ClassRoomRepository classRoomRepository;
    private final AcademicPeriodRepository academicPeriodRepository;

    @Override
    @Transactional
    public AcademicYear create(AcademicYear academicYear) {
        if (academicYearRepository.existsByYear(academicYear.getYear())) {
            throw new IllegalStateException("Academic year already registered: " + academicYear.getYear());
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
            throw new IllegalStateException("Academic year already registered: " + incoming.getYear());
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
                    "Cannot delete academic year with id " + id + " because it has classrooms linked to it.");
        }
        if (academicPeriodRepository.existsByAcademicYear(year)) {
            throw new IllegalStateException(
                    "Cannot delete academic year with id " + id + " because it has periods linked to it.");
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
    public AcademicYear findActive() {
        return academicYearRepository.findByActiveTrue()
                .orElseThrow(() -> new EntityNotFoundException("No active academic year found."));
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
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found with id: " + id));
    }
}
