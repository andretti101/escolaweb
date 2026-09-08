package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.AssessmentRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AcademicYearService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicPeriodServiceImpl implements AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final AssessmentRepository assessmentRepository;
    private final AcademicYearService academicYearService;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;

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
            throw new IllegalStateException("Cannot update a closed academic period with id: " + id);
        }

        validateAcademicYearExists(incoming.getAcademicYear());

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

    @Override
    @Transactional
    public AcademicPeriod close(Integer id) {
        AcademicPeriod period = findPeriodOrThrow(id);

        if (period.isClosed()) {
            throw new IllegalStateException("O período acadêmico com id " + id + " já está fechado.");
        }

        if (period.getEndDate() == null) {
            throw new IllegalStateException(
                    "Não foi possível fechar o período '" + period.getName()
                            + "': a data de término deve ser definida antes do fechamento.");
        }
        if (java.time.LocalDate.now().isBefore(period.getEndDate())) {
            throw new IllegalStateException(
                    "Não foi possível fechar o período '" + period.getName()
                            + "': a data de término (" + period.getEndDate()
                            + ") ainda não foi atingida.");
        }

        validateMinimumAssessments(period);

        period.setClosed(true);
        return period;
    }

    // ── Private helpers

    private void validateMinimumAssessments(AcademicPeriod period) {
        List<TeacherClassSubject> activeSubjects = teacherClassSubjectRepository.findAll();

        for (TeacherClassSubject tcs : activeSubjects) {
            if (tcs.getMinAssessmentsPerPeriod() != null) {
                long assessmentCount = assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, period);

                if (assessmentCount < tcs.getMinAssessmentsPerPeriod()) {
                    throw new IllegalStateException(
                            String.format("Não foi possível fechar o período '%s': a matéria '%s' na '%s' possui apenas %d avaliação(ões), mas exige no mínimo %d.",
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

    AcademicPeriod findPeriodOrThrow(Integer id) {
        return academicPeriodRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Academic period not found with id: " + id));
    }

    private void validateAcademicYearExists(AcademicYear academicYear) {
        if (academicYear == null || academicYear.getId() == null) {
            throw new IllegalArgumentException("Academic year is required.");
        }
        academicYearService.findById(academicYear.getId());
    }
}