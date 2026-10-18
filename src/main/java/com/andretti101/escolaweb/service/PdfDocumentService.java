package com.andretti101.escolaweb.service;

public interface PdfDocumentService {

    /**
     * Gera o Boletim Escolar Consolidado do aluno em formato PDF.
     *
     * @param studentId ID do aluno
     * @param academicYearId ID do ano letivo
     * @return array de bytes contendo o arquivo PDF
     */
    byte[] generateReportCardPdf(Integer studentId, Integer academicYearId);

    /**
     * Gera a Declaração de Matrícula oficial do aluno em formato PDF.
     *
     * @param enrollmentId ID da matrícula
     * @return array de bytes contendo o arquivo PDF
     */
    byte[] generateEnrollmentCertificatePdf(Integer enrollmentId);
}
