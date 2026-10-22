package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.response.ReportCardDTO;
import com.andretti101.escolaweb.dto.response.StudentAttendanceReportDTO;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.SchoolSettings;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.model.enums.StudentSituation;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfDocumentServiceImpl implements PdfDocumentService {

    private final ReportCardService reportCardService;
    private final SchoolSettingsService schoolSettingsService;
    private final EnrollmentService enrollmentService;
    private final AttendanceService attendanceService;
    private final StudentService studentService;
    private final EnrollmentRepository enrollmentRepository;
    private final SpringTemplateEngine templateEngine;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EXTENSIVE_DATE_FORMATTER = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT_BR);

    public record SubjectRowPdf(
            String subjectName,
            List<String> periodGrades,
            String finalAverage,
            int absences,
            String situationLabel,
            String situationBadgeClass
    ) {}

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReportCardPdf(Integer studentId, Integer academicYearId) {
        Student student = studentService.findById(studentId);
        ReportCardDTO reportCard = reportCardService.generateReportCard(studentId, academicYearId);
        SchoolSettings settings = schoolSettingsService.findSettings();
        StudentAttendanceReportDTO attendanceReport = attendanceService.getStudentAttendanceReport(studentId, academicYearId);

        Enrollment enrollment = enrollmentRepository.findByStudent(student).stream()
                .filter(e -> e.getClassRoom() != null && e.getClassRoom().getAcademicYear() != null
                        && e.getClassRoom().getAcademicYear().getId().equals(academicYearId))
                .sorted((e1, e2) -> Boolean.compare(e2.isActive(), e1.isActive()))
                .findFirst()
                .orElse(null);

        ClassRoom classRoom = enrollment != null ? enrollment.getClassRoom() : null;
        String schoolGrade = (classRoom != null && classRoom.getSchoolGrade() != null)
                ? classRoom.getSchoolGrade().getLabel()
                : "—";
        String shift = (classRoom != null && classRoom.getShift() != null)
                ? classRoom.getShift().getLabel()
                : "—";

        // Preserva a ordem cronológica dos períodos do boletim
        List<String> periodNames = reportCard.subjects().stream()
                .flatMap(s -> s.periodGrades().stream())
                .map(ReportCardDTO.PeriodGradeDTO::periodName)
                .distinct()
                .toList();

        // Mapeamento dos dados de cada disciplina
        List<SubjectRowPdf> subjectRows = new ArrayList<>();
        for (ReportCardDTO.SubjectSituationDTO s : reportCard.subjects()) {
            List<String> gradesList = new ArrayList<>();
            for (String pName : periodNames) {
                ReportCardDTO.PeriodGradeDTO pg = s.periodGrades().stream()
                        .filter(x -> pName.equals(x.periodName()))
                        .findFirst()
                        .orElse(null);
                if (pg != null && pg.average() != null) {
                    gradesList.add(String.format(PT_BR, "%.1f", pg.average()));
                } else {
                    gradesList.add("-");
                }
            }

            String finalAvgStr = "-";
            int gradesCount = (int) s.periodGrades().stream().filter(pg -> pg.average() != null).count();
            if (reportCard.totalPeriods() > 0 && gradesCount > 0 && (gradesCount == reportCard.totalPeriods()
                    || s.situation() == StudentSituation.APPROVED
                    || s.situation() == StudentSituation.FAILED)) {
                BigDecimal sum = s.periodGrades().stream()
                        .filter(pg -> pg.average() != null)
                        .map(ReportCardDTO.PeriodGradeDTO::average)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal finalAvg = sum.divide(BigDecimal.valueOf(reportCard.totalPeriods()), 1, RoundingMode.HALF_UP);
                finalAvgStr = String.format(PT_BR, "%.1f", finalAvg);
            }

            int absences = (attendanceReport != null && attendanceReport.absencesPerSubject() != null)
                    ? attendanceReport.absencesPerSubject().getOrDefault(s.subjectName(), 0)
                    : 0;

            String sitLabel;
            String badgeClass;
            if (s.situation() == StudentSituation.APPROVED) {
                sitLabel = "Aprovado";
                badgeClass = "badge-success";
            } else if (s.situation() == StudentSituation.FAILED) {
                sitLabel = "Reprovado";
                badgeClass = "badge-danger";
            } else {
                sitLabel = "Pendente";
                badgeClass = "badge-warning";
            }

            subjectRows.add(new SubjectRowPdf(
                    s.subjectName(),
                    gradesList,
                    finalAvgStr,
                    absences,
                    sitLabel,
                    badgeClass
            ));
        }

        // Situação Geral
        String genSitLabel;
        String genBadgeClass;
        if (reportCard.generalSituation() == StudentSituation.APPROVED) {
            genSitLabel = "APROVADO";
            genBadgeClass = "badge-success";
        } else if (reportCard.generalSituation() == StudentSituation.FAILED) {
            genSitLabel = "REPROVADO";
            genBadgeClass = "badge-danger";
        } else {
            genSitLabel = "EM CURSO / PENDENTE";
            genBadgeClass = "badge-warning";
        }

        // Frequência
        String generalFreqStr = reportCard.frequency() != null
                ? String.format(PT_BR, "%.2f%%", reportCard.frequency().generalFrequency())
                : "100.00%";
        String minFreqStr = reportCard.frequency() != null
                ? String.format(PT_BR, "%.2f%%", reportCard.frequency().minimumAttendance())
                : "70.00%";
        String freqSitLabel = (reportCard.frequency() != null && reportCard.frequency().situation() == StudentSituation.APPROVED)
                ? "APROVADO"
                : (reportCard.frequency() != null && reportCard.frequency().situation() == StudentSituation.FAILED ? "REPROVADO" : "PENDENTE");
        String freqBadgeClass = "APROVADO".equals(freqSitLabel) ? "badge-success" : ("REPROVADO".equals(freqSitLabel) ? "badge-danger" : "badge-warning");

        // Popula os dados consolidados do discente no contexto do Thymeleaf para o PDF
        Context context = new Context();
        context.setVariable("schoolSettings", settings);
        context.setVariable("reportCard", reportCard);
        context.setVariable("student", student);
        context.setVariable("schoolGrade", schoolGrade);
        context.setVariable("shift", shift);
        context.setVariable("periodNames", periodNames);
        context.setVariable("subjectRows", subjectRows);
        context.setVariable("generalSituationLabel", genSitLabel);
        context.setVariable("generalBadgeClass", genBadgeClass);
        context.setVariable("generalFrequencyStr", generalFreqStr);
        context.setVariable("minimumAttendanceStr", minFreqStr);
        context.setVariable("frequencySituationLabel", freqSitLabel);
        context.setVariable("frequencyBadgeClass", freqBadgeClass);
        context.setVariable("emissionDateTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        String renderedHtml = templateEngine.process("pdf/report-card-pdf", context);
        return renderHtmlToPdf(renderedHtml);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateEnrollmentCertificatePdf(Integer enrollmentId) {
        Enrollment enrollment = enrollmentService.findById(enrollmentId);
        SchoolSettings settings = schoolSettingsService.findSettings();
        Student student = enrollment.getStudent();
        ClassRoom classRoom = enrollment.getClassRoom();

        String birthDateStr = student.getBirthDate() != null
                ? student.getBirthDate().format(DATE_FORMATTER)
                : "Não informada";
        String enrollmentDateStr = enrollment.getEnrollmentDate() != null
                ? enrollment.getEnrollmentDate().format(DATE_FORMATTER)
                : "—";

        String schoolGrade = (classRoom != null && classRoom.getSchoolGrade() != null)
                ? classRoom.getSchoolGrade().getLabel()
                : "—";
        String shift = (classRoom != null && classRoom.getShift() != null)
                ? classRoom.getShift().getLabel()
                : "—";
        String academicYearStr = (classRoom != null && classRoom.getAcademicYear() != null)
                ? String.valueOf(classRoom.getAcademicYear().getYear())
                : String.valueOf(LocalDate.now().getYear());

        String verificationCode = generateVerificationCode(enrollment);

        Context context = new Context();
        context.setVariable("schoolSettings", settings);
        context.setVariable("enrollment", enrollment);
        context.setVariable("student", student);
        context.setVariable("classRoom", classRoom);
        context.setVariable("birthDateStr", birthDateStr);
        context.setVariable("enrollmentDateStr", enrollmentDateStr);
        context.setVariable("schoolGrade", schoolGrade);
        context.setVariable("shift", shift);
        context.setVariable("academicYearStr", academicYearStr);
        context.setVariable("verificationCode", verificationCode);
        context.setVariable("currentDateExtensive", LocalDate.now().format(EXTENSIVE_DATE_FORMATTER));
        context.setVariable("emissionDateTime", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        String renderedHtml = templateEngine.process("pdf/enrollment-certificate-pdf", context);
        return renderHtmlToPdf(renderedHtml);
    }

    private String generateVerificationCode(Enrollment enrollment) {
        try {
            int safeId = enrollment != null && enrollment.getId() != null ? enrollment.getId() : 0;
            String raw = safeId + ":" +
                    (enrollment != null && enrollment.getStudent() != null ? enrollment.getStudent().getRegistrationNumber() : "0") + ":" +
                    (enrollment != null ? enrollment.getEnrollmentDate() : "") + ":" +
                    (enrollment != null && enrollment.getClassRoom() != null ? enrollment.getClassRoom().getId() : "0");
            // Gera selo criptográfico SHA-256 para comprovação e verificação de autenticidade
        MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            String hex = sb.toString();
            return "EW-" + hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12);
        } catch (Exception e) {
            int safeId = enrollment != null && enrollment.getId() != null ? enrollment.getId() : 0;
            return "EW-VAL-" + String.format("%08X", safeId);
        }
    }

    // Converte o template HTML/CSS em arquivo binário PDF via FlyingSaucer e iText
    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao converter XHTML para PDF: {}", e.getMessage(), e);
            throw new IllegalStateException("Falha ao gerar documento PDF: " + e.getMessage(), e);
        }
    }
}
