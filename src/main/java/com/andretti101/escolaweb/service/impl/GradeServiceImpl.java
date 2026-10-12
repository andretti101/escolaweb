package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.GradeHistory;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
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

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeRepository gradeRepository;
    private final GradeHistoryRepository gradeHistoryRepository;
    private final com.andretti101.escolaweb.service.AuthenticatedUserService authenticatedUserService;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentService studentService;
    private final AssessmentService assessmentService;
    private final SchoolSettingsService schoolSettingsService;
    private final TeacherClassSubjectService teacherClassSubjectService;
    private final AttendanceService attendanceService;

    @Override
    @Transactional
    public Grade launch(Grade grade) {
        return launch(grade, null);
    }

    @Override
    @Transactional
    public Grade launch(Grade grade, String notes) {
        Student student = resolveStudent(grade.getStudent());
        Assessment assessment = resolveAssessment(grade.getAssessment());

        if (gradeRepository.existsByStudentAndAssessment(student, assessment)) {
            throw new IllegalStateException(
                    "Já existe uma nota lançada para o aluno '" + student.getName()
                    + "' na avaliação '" + assessment.getTitle() + "'. Para alterá-la, utilize a opção de edição de nota.");
        }

        validateActiveEnrollment(student, assessment.getTeacherClassSubject());
        validatePeriodOpen(assessment);
        validateGradeValue(grade.getValue());

        grade.setStudent(student);
        grade.setAssessment(assessment);

        Grade saved = gradeRepository.save(grade);

        updateStudentSituation(saved);

        return saved;
    }

    @Override
    @Transactional
    public Grade update(Integer id, Grade incoming) {
        return update(id, incoming, null);
    }

    @Override
    @Transactional
    public Grade update(Integer id, Grade incoming, String notes) {
        Grade existing = findGradeOrThrow(id);

        validatePeriodOpen(existing.getAssessment());
        validateGradeValue(incoming.getValue());

        BigDecimal previousValue = existing.getValue();

        existing.setValue(incoming.getValue());

        String cleanNotes = (notes != null && !notes.isBlank()) ? notes.trim() : null;
        boolean valueChanged = previousValue != null
                && incoming.getValue() != null
                && previousValue.compareTo(incoming.getValue()) != 0;

        if (valueChanged) {
            recordHistory(existing, previousValue, existing.getValue(), cleanNotes);
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

        List<Grade> allGrades = gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs);

        if (allGrades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        java.util.Map<com.andretti101.escolaweb.model.entity.AcademicPeriod, List<Grade>> gradesByPeriod = allGrades.stream()
                .filter(g -> g.getValue() != null && g.getAssessment() != null && g.getAssessment().getPeriod() != null)
                .collect(java.util.stream.Collectors.groupingBy(g -> g.getAssessment().getPeriod()));

        if (gradesByPeriod.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumOfPeriodAverages = BigDecimal.ZERO;

        for (List<Grade> periodGrades : gradesByPeriod.values()) {
            BigDecimal periodSum = periodGrades.stream()
                    .map(Grade::getValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal periodAvg = periodSum.divide(BigDecimal.valueOf(periodGrades.size()), 4, RoundingMode.HALF_UP);
            sumOfPeriodAverages = sumOfPeriodAverages.add(periodAvg);
        }

        int periodsWithGrades = gradesByPeriod.size();

        return sumOfPeriodAverages.divide(BigDecimal.valueOf(periodsWithGrades), 2, RoundingMode.CEILING);
    }

    private int getDivisorForPeriodType(com.andretti101.escolaweb.model.enums.AcademicPeriodType type) {
        if (type == null) return 1;
        switch (type) {
            case BIMESTER: return 4;
            case TRIMESTER: return 3;
            case QUADRIMESTER: return 3;
            case SEMESTER: return 2;
            case ANNUAL: return 1;
            default: return 1;
        }
    }

    // ── Private helpers

    private void updateStudentSituation(Grade grade) {
        TeacherClassSubject tcs = grade.getAssessment().getTeacherClassSubject();
        Student student = grade.getStudent();
        SchoolSettings settings = schoolSettingsService.findSettings();

        List<Grade> allGrades = gradeRepository.findByStudentAndAssessment_TeacherClassSubject(student, tcs);
        long periodsWithGrades = allGrades.stream()
                .filter(g -> g.getAssessment() != null && g.getAssessment().getPeriod() != null)
                .map(g -> g.getAssessment().getPeriod().getId())
                .distinct()
                .count();

        int divisor = getDivisorForPeriodType(settings.getPeriodType());

        if (periodsWithGrades < divisor) {
            allGrades.forEach(g -> g.setStudentSituation(StudentSituation.PENDING));
            return;
        }

        BigDecimal average = calculateAverage(student.getId(), tcs.getId());
        BigDecimal frequency = attendanceService.calculateFrequency(student.getId(), tcs.getId());

        boolean approvedByGrade = average.compareTo(settings.getMinimumGrade()) >= 0;
        boolean approvedByFrequency = frequency.compareTo(settings.getMinimumAttendance()) >= 0;

        StudentSituation situation = approvedByGrade && approvedByFrequency
                ? StudentSituation.APPROVED
                : StudentSituation.FAILED;

        allGrades.forEach(g -> g.setStudentSituation(situation));
    }

    private void recordHistory(Grade grade, BigDecimal previousValue, BigDecimal newValue, String notes) {
        GradeHistory history = GradeHistory.builder()
                .grade(grade)
                .previousGrade(previousValue)
                .newGrade(newValue)
                .notes(notes)
                .changedBy(authenticatedUserService.findAuthenticatedUserOrNull())
                .build();
        gradeHistoryRepository.save(history);
    }

    private void validatePeriodOpen(Assessment assessment) {
        if (assessment.getPeriod().isClosed()) {
            throw new IllegalStateException(
                    "O período acadêmico '" + assessment.getPeriod().getName()
                    + "' está fechado. Não é permitido lançar ou alterar notas.");
        }
    }

    private void validateGradeValue(BigDecimal value) {
        if (value == null) return;
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("O valor da nota deve estar entre 0,0 e 10,0. Verifique o valor digitado.");
        }
    }

    private void validateActiveEnrollment(Student student, TeacherClassSubject tcs) {
        if (!enrollmentRepository.existsByStudentAndClassRoomAndActiveTrue(student, tcs.getClassRoom())) {
            throw new IllegalStateException(
                    "O aluno '" + student.getName() + "' não possui matrícula ativa na turma '"
                    + tcs.getClassRoom().getName() + "'. Apenas alunos com matrícula ativa podem receber notas.");
        }
    }

    private Grade findGradeOrThrow(Integer id) {
        return gradeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nota não encontrada ou já removida."));
    }

    private Student resolveStudent(Student ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("A seleção do aluno é obrigatória.");
        }
        return studentService.findById(ref.getId());
    }

    private Assessment resolveAssessment(Assessment ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("A seleção da avaliação é obrigatória.");
        }
        return assessmentService.findById(ref.getId());
    }
}
