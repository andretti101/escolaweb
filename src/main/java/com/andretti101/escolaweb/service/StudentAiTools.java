package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.dto.response.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAiTools {

    private final AuthenticatedUserService authService;
    private final GradeService gradeService;
    private final AttendanceService attendanceService;
    private final ReportCardService reportCardService;
    private final AcademicYearService academicYearService;
    private final EnrollmentService enrollmentService;
    private final TeacherClassSubjectService teacherClassSubjectService;
    private final AcademicPeriodService academicPeriodService;
    private final SchoolSettingsService schoolSettingsService;

    @Tool(description = "Obtém as notas do aluno logado nas avaliações")
    public String getStudentGrades() {
        Student student = authService.getAuthenticatedStudent();
        var grades = gradeService.findByStudent(student.getId());
        String info = grades.stream()
                .map(g -> "Matéria: " + g.getAssessment().getTeacherClassSubject().getSubject().getName() + " - Avaliação: " + g.getAssessment().getTitle() + ", Nota: " + g.getValue())
                .collect(Collectors.joining("; "));
        return info.isEmpty() ? "Nenhuma nota encontrada." : info;
    }

    @Tool(description = "Obtém o registro de presença e faltas do aluno logado")
    public String getStudentAttendance() {
        Student student = authService.getAuthenticatedStudent();
        var attendances = attendanceService.findByStudent(student.getId());
        String info = attendances.stream()
                .map(a -> "Matéria: " + a.getLesson().getTeacherClassSubject().getSubject().getName() + " - Data: " + a.getLesson().getLessonDate() + ", Status: " + a.getStatus())
                .collect(Collectors.joining("; "));
        return info.isEmpty() ? "Nenhuma frequência registrada." : info;
    }

    @Tool(description = "Gera o boletim escolar do aluno logado com notas e situação geral")
    public String getStudentReportCard() {
        Student student = authService.getAuthenticatedStudent();
        AcademicYear year = academicYearService.findActive();
        if (year == null) return "Nenhum ano letivo ativo encontrado.";
        
        ReportCardDTO report = reportCardService.generateReportCard(student.getId(), year.getId());
        
        StringBuilder sb = new StringBuilder();
        sb.append("Boletim Escolar do aluno ").append(student.getName()).append(":\n");
        for (var sub : report.subjects()) {
            sb.append("- ").append(sub.subjectName())
              .append(": Nota ").append(sub.totalScore())
              .append(" / Alvo ").append(sub.targetScore())
              .append(" / Max ").append(sub.maxPossibleScore())
              .append(" (Situação: ").append(sub.situation()).append(")\n");
        }
        sb.append("\nFrequência: ").append(report.frequency().generalFrequency()).append("%");
        sb.append("\nSituação Geral: ").append(report.generalSituation());
        return sb.toString();
    }

    @Tool(description = "Calcula quantos pontos o aluno logado ainda precisa para cada matéria")
    public String getStudentPointsNeeded() {
        Student student = authService.getAuthenticatedStudent();
        AcademicYear year = academicYearService.findActive();
        if (year == null) return "Nenhum ano letivo ativo encontrado.";
        
        ReportCardDTO report = reportCardService.generateReportCard(student.getId(), year.getId());
        
        return report.subjects().stream().map(sub -> {
            BigDecimal diff = sub.targetScore().subtract(sub.totalScore());
            if (diff.compareTo(BigDecimal.ZERO) <= 0) {
                return sub.subjectName() + ": Aprovado (Nota: " + sub.totalScore() + ")";
            } else {
                return sub.subjectName() + ": Precisa de " + diff + " pontos.";
            }
        }).collect(Collectors.joining("\n"));
    }

    @Tool(description = "Informa quantos e quais colegas o aluno logado tem na sua turma atual")
    public String getStudentClassmates() {
        Student student = authService.getAuthenticatedStudent();
        var enrollments = enrollmentService.findByStudent(student.getId());
        var active = enrollments.stream().filter(Enrollment::isActive).findFirst();
        if (active.isEmpty()) return "Aluno não possui matrícula ativa.";
        
        Integer classRoomId = active.get().getClassRoom().getId();
        var classmateNames = enrollmentService.findByClassRoom(classRoomId).stream()
                .filter(Enrollment::isActive)
                .map(Enrollment::getStudent)
                .filter(Student::isActive)
                .filter(s -> !s.getId().equals(student.getId()))
                .map(Student::getName)
                .toList();
                
        if (classmateNames.isEmpty()) return "Você não tem colegas na turma " + active.get().getClassRoom().getName() + ".";
        
        String classmates = String.join(", ", classmateNames);
        return "Você tem " + classmateNames.size() + " colegas na turma " + active.get().getClassRoom().getName() + ". São eles: " + classmates;
    }

    @Tool(description = "Obtém os detalhes da matrícula atual do aluno logado (turma, série/ano e turno)")
    public String getStudentEnrollmentDetails() {
        Student student = authService.getAuthenticatedStudent();
        var enrollments = enrollmentService.findByStudent(student.getId());
        var active = enrollments.stream().filter(Enrollment::isActive).findFirst();
        if (active.isEmpty()) return "Aluno não possui matrícula ativa.";
        
        var classRoom = active.get().getClassRoom();
        return "Sua turma é " + classRoom.getName() + " e sua série é " + classRoom.getSchoolGrade() + " no turno " + classRoom.getShift() + ".";
    }

    @Tool(description = "Lista os professores e as matérias que eles lecionam para a turma atual do aluno logado")
    public String getStudentTeachers() {
        Student student = authService.getAuthenticatedStudent();
        var enrollments = enrollmentService.findByStudent(student.getId());
        var active = enrollments.stream().filter(Enrollment::isActive).findFirst();
        if (active.isEmpty()) return "Aluno não possui matrícula ativa.";
        
        Integer classRoomId = active.get().getClassRoom().getId();
        var teachers = teacherClassSubjectService.findByClassRoom(classRoomId);
        if (teachers == null || teachers.isEmpty()) return "Nenhum professor cadastrado para a sua turma.";
        
        return teachers.stream()
                .map(tcs -> "Professor(a): " + tcs.getTeacher().getName() + " - Matéria: " + tcs.getSubject().getName())
                .collect(Collectors.joining("; "));
    }

    @Tool(description = "Obtém o ano letivo atual e seus períodos acadêmicos (com datas de início e fim)")
    public String getAcademicYearAndPeriods() {
        AcademicYear year = academicYearService.findActive();
        if (year == null) return "Nenhum ano letivo ativo encontrado.";
        
        var periods = academicPeriodService.findByAcademicYear(year.getId());
        String periodsInfo = periods.stream()
                .map(p -> p.getName() + " (De " + p.getStartDate() + " até " + p.getEndDate() + ")")
                .collect(Collectors.joining("; "));
        
        return "Ano Letivo: " + year.getYear() + " (De " + year.getStartDate() + " até " + year.getEndDate() + ")\nPeríodos: " + (periodsInfo.isEmpty() ? "Nenhum período cadastrado." : periodsInfo);
    }

    @Tool(description = "Analisa qual a melhor e a pior matéria do aluno logado")
    public String getStudentSubjectAnalysis() {
        Student student = authService.getAuthenticatedStudent();
        AcademicYear year = academicYearService.findActive();
        if (year == null) return "Nenhum ano letivo ativo encontrado.";
        
        ReportCardDTO report = reportCardService.generateReportCard(student.getId(), year.getId());
        if (report.subjects().isEmpty()) return "Sem dados de matérias suficientes.";
        
        var bestGrade = report.subjects().stream().max(Comparator.comparing(ReportCardDTO.SubjectSituationDTO::totalScore)).get();
        var worstGrade = report.subjects().stream().min(Comparator.comparing(ReportCardDTO.SubjectSituationDTO::totalScore)).get();
        
        String attendanceInfo = "";
        try {
            var attendanceReport = attendanceService.getStudentAttendanceReport(student.getId(), year.getId());
            if (attendanceReport.frequencyPerSubject() != null && !attendanceReport.frequencyPerSubject().isEmpty()) {
                var bestAtt = attendanceReport.frequencyPerSubject().entrySet().stream().max(Map.Entry.comparingByValue()).get();
                var worstAtt = attendanceReport.frequencyPerSubject().entrySet().stream().min(Map.Entry.comparingByValue()).get();
                attendanceInfo = "\nMelhor frequência: " + bestAtt.getKey() + " (" + bestAtt.getValue() + "%)\n" +
                                 "Pior frequência: " + worstAtt.getKey() + " (" + worstAtt.getValue() + "%)";
            }
        } catch (Exception e) {}
        
        return "Melhor matéria em notas: " + bestGrade.subjectName() + " (" + bestGrade.totalScore() + " pontos)\n" +
               "Pior matéria em notas: " + worstGrade.subjectName() + " (" + worstGrade.totalScore() + " pontos)" + attendanceInfo;
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
