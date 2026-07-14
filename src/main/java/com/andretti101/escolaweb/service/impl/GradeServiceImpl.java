package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.GradeHistory;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.GradeHistoryRepository;
import com.andretti101.escolaweb.repository.GradeRepository;
import com.andretti101.escolaweb.service.AssessmentService;
import com.andretti101.escolaweb.service.AttendanceService;
import com.andretti101.escolaweb.service.GradeService;
import com.andretti101.escolaweb.service.SchoolSettingsService;
import com.andretti101.escolaweb.service.StudentService;
import com.andretti101.escolaweb.service.TeacherClassSubjectService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final StudentService studentService;
    private final AssessmentService assessmentService;
    private final SchoolSettingsService schoolSettingsService;
    private final TeacherClassSubjectService teacherClassSubjectService;
    private final AttendanceService attendanceService;

    @Override
    @Transactional
    public Grade launch(Grade grade) {
        Student student = resolveStudent(grade.getStudent());
        Assessment assessment = resolveAssessment(grade.getAssessment());

        if (gradeRepository.existsByStudentAndAssessment(student, assessment)) {
            throw new IllegalStateException(
                    "Grade for student with id " + student.getId()
                    + " on assessment with id " + assessment.getId() + " is already registered.");
        }

        validatePeriodOpen(assessment);
        validateGradeValue(grade.getValue());

        grade.setStudent(student);
        grade.setAssessment(assessment);

        Grade saved = gradeRepository.save(grade);

        recordHistory(saved, null, saved.getValue());

        updateStudentSituation(saved);

        return saved;
    }

    @Override
    @Transactional
    public Grade update(Integer id, Grade incoming) {
        Grade existing = findGradeOrThrow(id);

        validatePeriodOpen(existing.getAssessment());
        validateGradeValue(incoming.getValue());

        BigDecimal previousValue = existing.getValue();

        existing.setValue(incoming.getValue());

        if (!Objects.equals(previousValue, incoming.getValue())) {
            recordHistory(existing, previousValue, existing.getValue());
        }

        updateStudentSituation(existing);

        return existing;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        findGradeOrThrow(id);
        gradeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Grade findById(Integer id) {
        return findGradeOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findAll() {
        return gradeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByStudent(Integer studentId) {
        Student student = studentService.findById(studentId);
        return gradeRepository.findByStudent(student);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Grade> findByAssessment(Integer assessmentId) {
        Assessment assessment = assessmentService.findById(assessmentId);
        return gradeRepository.findByAssessment(assessment);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateAverage(Integer studentId, Integer teacherClassSubjectId) {
        Student student = studentService.findById(studentId);
        TeacherClassSubject tcs = teacherClassSubjectService.findById(teacherClassSubjectId);

        List<BigDecimal> values = gradeRepository
                .findByStudentAndAssessment_TeacherClassSubject(student, tcs)
                .stream()
                .map(Grade::getValue)
                .filter(Objects::nonNull)
                .toList();

        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    // ── Private helpers

    private void updateStudentSituation(Grade grade) {
        TeacherClassSubject tcs = grade.getAssessment().getTeacherClassSubject();
        Student student = grade.getStudent();
        SchoolSettings settings = schoolSettingsService.findSettings();

        BigDecimal average = calculateAverage(student.getId(), tcs.getId());
        BigDecimal frequency = attendanceService.calculateFrequency(student.getId(), tcs.getId());

        boolean approvedByGrade = average.compareTo(settings.getMinimumGrade()) >= 0;
        boolean approvedByFrequency = frequency.compareTo(settings.getMinimumAttendance()) >= 0;

        grade.setStudentSituation(
                approvedByGrade && approvedByFrequency
                        ? StudentSituation.APPROVED
                        : StudentSituation.FAILED);
    }

    private void recordHistory(Grade grade, BigDecimal previousValue, BigDecimal newValue) {
        GradeHistory history = GradeHistory.builder()
                .grade(grade)
                .previousGrade(previousValue)
                .newGrade(newValue)
                .build();
        gradeHistoryRepository.save(history);
    }

    private void validatePeriodOpen(Assessment assessment) {
        if (assessment.getPeriod().isClosed()) {
            throw new IllegalStateException(
                    "Academic period '" + assessment.getPeriod().getName()
                    + "' is closed. Grades cannot be modified.");
        }
    }

    private void validateGradeValue(BigDecimal value) {
        if (value == null) return;
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Grade value must be between 0 and 10.");
        }
    }

    private Grade findGradeOrThrow(Integer id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Grade not found with id: " + id));
    }

    private Student resolveStudent(Student ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Student is required.");
        }
        return studentService.findById(ref.getId());
    }

    private Assessment resolveAssessment(Assessment ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Assessment is required.");
        }
        return assessmentService.findById(ref.getId());
    }
}
