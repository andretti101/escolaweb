package com.andretti101.escolaweb.service;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.Enrollment;
import com.andretti101.escolaweb.model.entity.Student;
import com.andretti101.escolaweb.repository.AcademicYearRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.StudentRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PdfDocumentServiceImplTest {

    @Autowired
    private PdfDocumentService pdfDocumentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("Gera Boletim Escolar em PDF válido com cabeçalho PDF e texto do aluno")
    void generateReportCardPdf_success() throws IOException {
        Student student = studentRepository.findAll().stream()
                .filter(s -> "lucas.almeida@example.com".equals(s.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Lucas Almeida não encontrado no seed"));

        AcademicYear activeYear = academicYearRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Ano letivo ativo não encontrado"));

        byte[] pdfBytes = pdfDocumentService.generateReportCardPdf(student.getId(), activeYear.getId());

        assertNotNull(pdfBytes, "O array de bytes do PDF não deve ser nulo");
        assertTrue(pdfBytes.length > 500, "O tamanho do PDF deve ser consistente");

        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8), StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"), "O arquivo gerado deve conter a assinatura '%PDF-'");

        // Deep verification: extrai texto das páginas do PDF
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertTrue(reader.getNumberOfPages() >= 1, "O PDF do boletim deve possuir ao menos 1 página");
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String pageText = extractor.getTextFromPage(1);
            assertNotNull(pageText);
            assertTrue(pageText.contains(student.getName()), "O PDF do boletim deve conter o nome do aluno");
            assertTrue(pageText.toLowerCase().contains("boletim escolar oficial"), "O PDF do boletim deve conter o título oficial");
        }
    }

    @Test
    @DisplayName("Gera Declaração de Matrícula em PDF ativa com código de autenticação")
    void generateEnrollmentCertificatePdf_success() throws IOException {
        Student student = studentRepository.findAll().stream()
                .filter(s -> "lucas.almeida@example.com".equals(s.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Lucas Almeida não encontrado no seed"));

        Enrollment enrollment = enrollmentRepository.findByStudent(student).stream()
                .filter(Enrollment::isActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Matrícula ativa não encontrada para Lucas"));

        byte[] pdfBytes = pdfDocumentService.generateEnrollmentCertificatePdf(enrollment.getId());

        assertNotNull(pdfBytes, "O array de bytes da declaração não deve ser nulo");
        assertTrue(pdfBytes.length > 500, "O tamanho da declaração em PDF deve ser consistente");

        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 8), StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"), "O arquivo gerado deve conter a assinatura '%PDF-'");

        // Deep verification: extrai texto do PDF
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            assertEquals(1, reader.getNumberOfPages(), "A declaração deve ocupar 1 página A4");
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String pageText = extractor.getTextFromPage(1);
            assertNotNull(pageText);
            assertTrue(pageText.contains(student.getName()), "Deve conter o nome do aluno");
            assertTrue(pageText.toLowerCase().contains("matr") && pageText.toLowerCase().contains("escolar"), "Deve conter o título do documento");
            assertTrue(pageText.contains("EW-"), "Deve conter o código de autenticação gerado EW-xxxx-xxxx-xxxx");
            assertTrue(pageText.toLowerCase().contains("ativo") || pageText.toLowerCase().contains("regularmente"), "Declaração de matrícula ativa deve atestar vínculo ativo");
        }
    }

    @Test
    @DisplayName("Gera Declaração de Matrícula para matrícula inativa com redação apropriada")
    void generateEnrollmentCertificatePdf_inactiveEnrollment_rendersInactiveText() throws IOException {
        Student student = studentRepository.findAll().stream()
                .filter(s -> "lucas.almeida@example.com".equals(s.getEmail()))
                .findFirst()
                .orElseThrow();

        Enrollment enrollment = enrollmentRepository.findByStudent(student).stream()
                .filter(Enrollment::isActive)
                .findFirst()
                .orElseThrow();

        // Altera para inativo temporariamente
        enrollment.setActive(false);
        enrollmentRepository.saveAndFlush(enrollment);

        byte[] pdfBytes = pdfDocumentService.generateEnrollmentCertificatePdf(enrollment.getId());

        assertNotNull(pdfBytes);
        try (PdfReader reader = new PdfReader(pdfBytes)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String pageText = extractor.getTextFromPage(1);
            assertTrue(pageText.toLowerCase().contains("inativa"), "Declaração de matrícula inativa deve constar condição inativa");
        }
    }

    @Test
    @DisplayName("Tentativa de gerar boletim para aluno inexistente lança exceção")
    void generateReportCardPdf_nonExistentStudent_throwsException() {
        AcademicYear activeYear = academicYearRepository.findByActiveTrue().orElseThrow();
        assertThrows(Exception.class, () -> pdfDocumentService.generateReportCardPdf(99999, activeYear.getId()));
    }

    @Test
    @DisplayName("Tentativa de gerar declaração para matrícula inexistente lança exceção")
    void generateEnrollmentCertificatePdf_nonExistentEnrollment_throwsException() {
        assertThrows(Exception.class, () -> pdfDocumentService.generateEnrollmentCertificatePdf(99999));
    }
}
