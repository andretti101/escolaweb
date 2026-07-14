package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicPeriod;
import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.repository.AssessmentRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import com.andretti101.escolaweb.service.AcademicPeriodService;
import com.andretti101.escolaweb.service.AssessmentService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final TeacherClassSubjectService teacherClassSubjectService;
    private final AcademicPeriodService academicPeriodService;

    @Override
    @Transactional
    public Assessment create(Assessment assessment) {
        TeacherClassSubject tcs = resolveTcs(assessment.getTeacherClassSubject());
        AcademicPeriod period = resolvePeriod(assessment.getPeriod());

        validatePeriodOpen(period);
        validateDate(assessment);

        assessment.setTeacherClassSubject(tcs);
        assessment.setPeriod(period);

        return assessmentRepository.save(assessment);
    }

    @Override
    @Transactional
    public Assessment update(Integer id, Assessment incoming) {
        Assessment existing = findAssessmentOrThrow(id);

        validatePeriodOpen(existing.getPeriod());

        TeacherClassSubject tcs = resolveTcs(incoming.getTeacherClassSubject());
        AcademicPeriod period = resolvePeriod(incoming.getPeriod());

        validatePeriodOpen(period);
        validateDate(incoming);

        existing.setTitle(incoming.getTitle());
        existing.setDescription(incoming.getDescription());
        existing.setDate(incoming.getDate());
        existing.setAssessmentType(incoming.getAssessmentType());
        existing.setTeacherClassSubject(tcs);
        existing.setPeriod(period);

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Assessment assessment = findAssessmentOrThrow(id);

        validatePeriodOpen(assessment.getPeriod());

        if (gradeRepository.existsByAssessment(assessment)) {
            throw new IllegalStateException(
                    "Cannot delete assessment with id " + id + " because it has grades registered.");
        }

        assessmentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Assessment findById(Integer id) {
        return findAssessmentOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> findAll() {
        return assessmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> findByTeacherClassSubject(Integer teacherClassSubjectId) {
        TeacherClassSubject tcs = teacherClassSubjectService.findById(teacherClassSubjectId);
        return assessmentRepository.findByTeacherClassSubject(tcs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> findByPeriod(Integer periodId) {
        AcademicPeriod period = academicPeriodService.findById(periodId);
        return assessmentRepository.findByPeriod(period);
    }

    Assessment findAssessmentOrThrow(Integer id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Assessment not found with id: " + id));
    }

    private TeacherClassSubject resolveTcs(TeacherClassSubject ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("TeacherClassSubject is required.");
        }
        return teacherClassSubjectService.findById(ref.getId());
    }

    private AcademicPeriod resolvePeriod(AcademicPeriod ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Academic period is required.");
        }
        return academicPeriodService.findById(ref.getId());
    }

    private void validatePeriodOpen(AcademicPeriod period) {
        if (period.isClosed()) {
            throw new IllegalStateException(
                    "Academic period '" + period.getName() + "' is closed. No changes are allowed.");
        }
    }

    private void validateDate(Assessment assessment) {
        if (assessment.getDate() == null) {
            throw new IllegalArgumentException("Assessment date is required.");
        }
    }
}
