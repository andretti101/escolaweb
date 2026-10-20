package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.Assessment;
import com.andretti101.escolaweb.model.entity.Grade;
import com.andretti101.escolaweb.model.entity.GradeHistory;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.entity.TeacherClassSubject;
import com.andretti101.escolaweb.model.enums.GradeKind;
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
    private final com.andretti101.escolaweb.service.NotificationPublisherService notificationPublisherService;

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
        validateGradeValue(grade.getRecoveryValue());

        grade.setStudent(student);
        grade.setAssessment(assessment);

        Grade saved = gradeRepository.save(grade);

        updateStudentSituation(saved);

        dispatchGradeNotification(saved, saved.getValue(), "Nova nota lançada em %s: %s na avaliação \"%s\"", "Nova Nota Lançada");

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
            recordHistory(existing, GradeKind.ORIGINAL, previousValue, existing.getValue(), cleanNotes);
            dispatchGradeNotification(existing, existing.getValue(), "Sua nota em %s foi atualizada para %s na avaliação \"%s\"", "Nota Atualizada");
        }

        updateStudentSituation(existing);

        return existing;
    }

    @Override
    @Transactional
    public Grade launchRecovery(Integer id, BigDecimal recoveryValue, String notes) {
        Grade existing = findGradeOrThrow(id);

        validatePeriodOpen(existing.getAssessment());
        if (recoveryValue == null) {
            throw new IllegalArgumentException("O valor da nota de recuperação é obrigatório.");
        }
        validateGradeValue(recoveryValue);

        BigDecimal previousRecovery = existing.getRecoveryValue();
        existing.setRecoveryValue(recoveryValue);

        String cleanNotes = (notes != null && !notes.isBlank()) ? notes.trim() : null;
        if (previousRecovery == null) {
            dispatchGradeNotification(existing, recoveryValue,
                    "Nota de recuperação lançada em %s: %s na avaliação \"%s\"",
                    "Nota de Recuperação Lançada");
        } else if (previousRecovery.compareTo(recoveryValue) != 0) {
            recordHistory(existing, GradeKind.RECOVERY, previousRecovery, recoveryValue, cleanNotes);
            dispatchGradeNotification(existing, recoveryValue,
                    "Sua nota de recuperação em %s foi atualizada para %s na avaliação \"%s\"",
                    "Nota de Recuperação Atualizada");
        }

        updateStudentSituation(existing);

        return existing;
    }

    @Override
    @Transactional
    public Grade removeRecovery(Integer id, String notes) {
        Grade existing = findGradeOrThrow(id);

        validatePeriodOpen(existing.getAssessment());

        BigDecimal previousRecovery = existing.getRecoveryValue();
        if (previousRecovery != null) {
            existing.setRecoveryValue(null);
            String cleanNotes = (notes != null && !notes.isBlank()) ? notes.trim() : null;
            recordHistory(existing, GradeKind.RECOVERY, previousRecovery, null, cleanNotes);
            updateStudentSituation(existing);
        }

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
    public org.springframework.data.domain.Page<Grade> findAll(org.springframework.data.domain.Pageable pageable) {
        return gradeRepository.findAll(pageable);
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
                .filter(g -> g.getEffectiveValue() != null && g.getAssessment() != null && g.getAssessment().getPeriod() != null)
                .collect(java.util.stream.Collectors.groupingBy(g -> g.getAssessment().getPeriod()));

        if (gradesByPeriod.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumOfPeriodAverages = BigDecimal.ZERO;

        for (List<Grade> periodGrades : gradesByPeriod.values()) {
            BigDecimal periodSum = periodGrades.stream()
                    .map(Grade::getEffectiveValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal periodAvg = periodSum.divide(BigDecimal.valueOf(periodGrades.size()), 4, RoundingMode.HALF_UP);
            sumOfPeriodAverages = sumOfPeriodAverages.add(periodAvg);
        }

        int periodsWithGrades = gradesByPeriod.size();

        return sumOfPeriodAverages.divide(BigDecimal.valueOf(periodsWithGrades), 2, RoundingMode.CEILING);
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

        int divisor = (tcs.getClassRoom() != null && tcs.getClassRoom().getPeriodType() != null)
                ? tcs.getClassRoom().getPeriodType().getDivisor()
                : 1;

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

    private void recordHistory(Grade grade, GradeKind kind, BigDecimal previousValue, BigDecimal newValue, String notes) {
        GradeHistory history = GradeHistory.builder()
                .grade(grade)
                .gradeKind(kind != null ? kind : GradeKind.ORIGINAL)
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

    private void dispatchGradeNotification(Grade grade, BigDecimal val, String messageTemplate, String title) {
        if (notificationPublisherService == null || grade.getStudent() == null) return;
        try {
            String subjectName = (grade.getAssessment() != null
                    && grade.getAssessment().getTeacherClassSubject() != null
                    && grade.getAssessment().getTeacherClassSubject().getSubject() != null)
                    ? grade.getAssessment().getTeacherClassSubject().getSubject().getName()
                    : "Disciplina";
            String assessmentTitle = (grade.getAssessment() != null && grade.getAssessment().getTitle() != null)
                    ? grade.getAssessment().getTitle()
                    : "Avaliação";

            com.andretti101.escolaweb.dto.response.NotificationMessageDTO notif =
                    com.andretti101.escolaweb.dto.response.NotificationMessageDTO.builder()
                            .id(java.util.UUID.randomUUID().toString())
                            .type("GRADE_LAUNCHED")
                            .title(title)
                            .message(String.format(messageTemplate, subjectName, val != null ? val.toString() : "-", assessmentTitle))
                            .linkUrl("/aluno/notas")
                            .timestamp(java.time.LocalDateTime.now())
                            .build();

            notificationPublisherService.notifyStudent(grade.getStudent().getId(), notif);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(GradeServiceImpl.class)
                    .warn("Erro ao publicar notificação de nota: {}", e.getMessage());
        }
    }
}
