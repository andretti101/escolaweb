package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${password-reset.base-url}")
    private String baseUrl;

    @Override
    public void sendPasswordResetEmail(String to, String userName, String token) {
        String resetLink = baseUrl + "/reset-password.html?token=" + token;
        String htmlContent = buildResetEmailHtml(userName, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@escolaweb.com");
            helper.setTo(to);
            helper.setSubject("EscolaWEB — Redefinição de Senha");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
            throw new IllegalStateException(
                    "Falha ao enviar e-mail de redefinição de senha.", e);
        }
    }

    // ── Private helpers

    private String buildResetEmailHtml(String userName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Public Sans', sans-serif; background-color: #f5f5f9; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 16px;
                                     overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }
                        .header { background: linear-gradient(135deg, #696cff, #8592ff); padding: 30px; text-align: center; }
                        .header h1 { color: #ffffff; margin: 0; font-size: 24px; }
                        .body { padding: 30px; }
                        .body p { color: #566a7f; font-size: 15px; line-height: 1.6; }
                        .btn { display: inline-block; background: #696cff; color: #ffffff !important;
                               text-decoration: none; padding: 12px 30px; border-radius: 10px;
                               font-size: 15px; font-weight: 600; margin: 20px 0; }
                        .footer { padding: 20px 30px; text-align: center; background: #f5f5f9; }
                        .footer p { color: #a1acb8; font-size: 12px; margin: 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>EscolaWEB</h1>
                        </div>
                        <div class="body">
                            <p>Olá, <strong>%s</strong>!</p>
                            <p>Recebemos uma solicitação para redefinir a senha da sua conta no EscolaWEB.</p>
                            <p>Clique no botão abaixo para criar uma nova senha:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="btn">Redefinir Senha</a>
                            </p>
                            <p>Este link é válido por <strong>15 minutos</strong>.
                               Após esse período, será necessário solicitar um novo link.</p>
                            <p>Se você não solicitou a redefinição de senha, ignore este e-mail.
                               Sua senha permanecerá a mesma.</p>
                        </div>
                        <div class="footer">
                            <p>© 2026 EscolaWEB — Sistema de Gestão Escolar</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(userName, resetLink);
    }
}
