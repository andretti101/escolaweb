package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.repository.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherAiTools {

    private final AuthenticatedUserService authService;
    private final TeacherClassSubjectService tcsService;
    private final StudentRepository studentRepository;
    private final AssessmentRepository assessmentRepository;
    private final GradeRepository gradeRepository;
    private final AcademicYearService yearService;
    private final AcademicPeriodService periodService;
    private final SchoolSettingsService schoolSettingsService;
    private final TeacherService teacherService;

    @Tool(description = "Lista todas as turmas e matérias que o professor logado leciona")
    public String getTeacherClasses() {
        Teacher teacher = authService.getAuthenticatedTeacher();
        var classes = tcsService.findByTeacher(teacher.getId());
        if (classes.isEmpty()) return "Você não tem turmas atribuídas.";
        return classes.stream().map(tcs -> 
            "Turma: " + tcs.getClassRoom().getName() + " - Matéria: " + tcs.getSubject().getName()
        ).collect(Collectors.joining("\n"));
    }

    @Tool(description = "Mostra os alunos de uma turma pelo seu classRoomId")
    public String getTeacherClassStudents(Integer classRoomId) {
        Teacher teacher = authService.getAuthenticatedTeacher();
        var classes = tcsService.findByTeacher(teacher.getId());
        boolean hasAccess = classes.stream().anyMatch(t -> t.getClassRoom().getId().equals(classRoomId));
        if (!hasAccess) return "Acesso negado: Você não leciona nesta turma.";
        var students = studentRepository.findByClassroomId(classRoomId);
        return "Alunos na turma:\n" + students.stream().map(Student::getName).collect(Collectors.joining("\n"));
    }

    @Tool(description = "Mostra os piores desempenhos nas matérias do professor")
    public String getTeacherStudentPerformance() {
        Teacher teacher = authService.getAuthenticatedTeacher();
        var classes = tcsService.findByTeacher(teacher.getId());
        StringBuilder sb = new StringBuilder();
        for (var tcs : classes) {
            sb.append("Turma: ").append(tcs.getClassRoom().getName())
              .append(" - ").append(tcs.getSubject().getName()).append("\n");
            var assessments = assessmentRepository.findByTeacherClassSubject(tcs);
            for (var a : assessments) {
                var grades = gradeRepository.findByAssessment(a);
                var badGrades = grades.stream()
                    .filter(g -> g.getValue().compareTo(new BigDecimal("6.0")) < 0)
                    .collect(Collectors.toList());
                if (!badGrades.isEmpty()) {
                    sb.append("  Avaliação: ").append(a.getTitle()).append(" - Alunos com nota baixa:\n");
                    badGrades.forEach(g -> sb.append("    - ").append(g.getStudent().getName()).append(": ").append(g.getValue()).append("\n"));
                }
            }
        }
        if (sb.isEmpty()) return "Nenhum aluno com baixo desempenho.";
        return sb.toString();
    }

    @Tool(description = "Verifica avaliações pendentes por turma/período")
    public String getTeacherPendingAssessments() {
        Teacher teacher = authService.getAuthenticatedTeacher();
        AcademicYear year = yearService.findActive();
        if (year == null) return "Sem ano letivo ativo.";
        
        var periods = periodService.findByAcademicYear(year.getId());
        var classes = tcsService.findByTeacher(teacher.getId());
        StringBuilder sb = new StringBuilder();
        for (var tcs : classes) {
            for (var period : periods) {
                long count = assessmentRepository.countByTeacherClassSubjectAndPeriod(tcs, period);
                long min = tcs.getMinAssessmentsPerPeriod();
                if (count < min) {
                    sb.append("Turma: ").append(tcs.getClassRoom().getName())
                      .append(", Matéria: ").append(tcs.getSubject().getName())
                      .append(", Período: ").append(period.getName())
                      .append(" - Pendentes: ").append(min - count).append(" avaliações.\n");
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "Todas as avaliações mínimas foram criadas.";
    }

    @Tool(description = "Lista os outros professores da escola (colegas)")
    public String getOtherTeachers() {
        Teacher teacher = authService.getAuthenticatedTeacher();
        var teachers = teacherService.findAllActive().stream()
            .filter(t -> !t.getId().equals(teacher.getId()))
            .map(Teacher::getName)
            .collect(Collectors.toList());
        if (teachers.isEmpty()) return "Nenhum outro professor ativo encontrado.";
        return "Você tem " + teachers.size() + " colegas professores: " + String.join(", ", teachers);
    }

    @Tool(description = "Obtém o ano letivo atual e seus períodos acadêmicos (com datas de início e fim)")
    public String getAcademicYearAndPeriods() {
        AcademicYear year = yearService.findActive();
        if (year == null) return "Nenhum ano letivo ativo encontrado.";
        
        var periods = periodService.findByAcademicYear(year.getId());
        String periodsInfo = periods.stream()
                .map(p -> p.getName() + " (De " + p.getStartDate() + " até " + p.getEndDate() + ")")
                .collect(Collectors.joining("; "));
        
        return "Ano Letivo: " + year.getYear() + " (De " + year.getStartDate() + " até " + year.getEndDate() + ")\nPeríodos: " + (periodsInfo.isEmpty() ? "Nenhum período cadastrado." : periodsInfo);
    }

    @Tool(description = "Obtém as configurações escolares definidas pela escola")
    public String getSchoolSettings() {
        SchoolSettings settings = schoolSettingsService.findSettings();
        if (settings == null) return "Configurações escolares não encontradas.";
        return "Escola: " + settings.getSchoolName() +
               "\nMédia mínima para aprovação: " + settings.getMinimumGrade() +
               "\nFrequência mínima (%): " + settings.getMinimumAttendance() +
               "\nTipo de período: " + settings.getPeriodType();
    }
}
