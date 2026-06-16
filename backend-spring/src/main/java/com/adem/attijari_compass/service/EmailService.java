package com.adem.attijari_compass.service;

import com.adem.attijari_compass.exception.EmailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private static final String LOGO_CID = "attijari-logo";
    private static final String LOGO_PATH = "mail/logo.png";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${frontend.url:http://127.0.0.1:4200}")
    private String frontendUrl;

    public void sendAdminVerificationCode(String to, String fullName, String code) {
        sendTemplate(to, "Code de vérification – Attijari Compass", EmailTemplate.builder()
                .fullName(displayName(fullName))
                .title("Code de vérification")
                .subtitle("Finalisez votre demande d’accès administrateur.")
                .mainMessage("Saisissez ce code dans l’interface Attijari Compass pour confirmer votre adresse e-mail.")
                .verificationCode(code)
                .warningMessage("Ce code est valable pendant 10 minutes. Ne partagez jamais ce code avec une autre personne.")
                .build());
    }

    public void sendUserVerificationCode(String to, String fullName, String code) {
        sendTemplate(to, "Code de vérification – Attijari Compass", EmailTemplate.builder()
                .fullName(displayName(fullName))
                .title("Activez votre compte")
                .subtitle("Bienvenue dans Attijari Compass.")
                .mainMessage("Saisissez ce code dans l’application pour activer votre compte et accéder à votre espace financier.")
                .verificationCode(code)
                .warningMessage("Ce code est valable pendant 10 minutes. Ne partagez jamais ce code avec une autre personne.")
                .build());
    }

    public void sendAdminApprovalEmail(String to, String fullName) {
        sendTemplate(to, "Votre accès administrateur a été approuvé – Attijari Compass", EmailTemplate.builder()
                .fullName(displayName(fullName))
                .title("Accès administrateur approuvé")
                .subtitle("Votre demande de création d’un compte administrateur a été approuvée.")
                .mainMessage("Vous pouvez désormais accéder à l’espace Back Office Attijari Compass.")
                .buttonText("Accéder à la plateforme")
                .buttonUrl(loginUrl())
                .build());
    }

    public void sendAdminRejectionEmail(String to, String fullName, String reason) {
        sendTemplate(to, "Demande de compte administrateur refusée – Attijari Compass", EmailTemplate.builder()
                .fullName(displayName(fullName))
                .title("Demande administrateur refusée")
                .subtitle("Votre demande de création d’un compte administrateur n’a pas été approuvée.")
                .mainMessage("Motif : " + safeReason(reason))
                .warningMessage("Vous pouvez contacter un administrateur si vous pensez qu’il s’agit d’une erreur.")
                .build());
    }

    public void sendAccountRestoreVerificationCode(String to, String fullName, String code) {
        sendTemplate(to, "Code de vérification – Restauration de compte Attijari Compass", EmailTemplate.builder()
                .fullName(displayName(fullName))
                .title("Restauration de compte")
                .subtitle("Confirmez votre adresse e-mail pour poursuivre la demande.")
                .mainMessage("Saisissez ce code dans l’interface de restauration de compte Attijari Compass.")
                .verificationCode(code)
                .warningMessage("Ce code est valable pendant 10 minutes. Ne partagez jamais ce code avec une autre personne.")
                .build());
    }

    public void sendAccountRestoreApprovedEmail(String to, String fullName) {
        sendTemplate(to, "Compte restauré avec succès – Attijari Compass", EmailTemplate.builder()
                .fullName(displayName(fullName))
                .title("Compte restauré avec succès")
                .subtitle("Votre demande de restauration de compte a été approuvée.")
                .mainMessage("Votre compte est de nouveau actif et accessible.")
                .buttonText("Se connecter")
                .buttonUrl(loginUrl())
                .build());
    }

    public void sendAccountRestoreRejectedEmail(String to, String fullName, String reason) {
        sendTemplate(to, "Demande de restauration refusée – Attijari Compass", EmailTemplate.builder()
                .fullName(displayName(fullName))
                .title("Demande de restauration refusée")
                .subtitle("Votre demande de restauration de compte n’a pas été approuvée.")
                .mainMessage("Motif : " + safeReason(reason))
                .warningMessage("Vous pouvez contacter le support si vous souhaitez plus d’informations.")
                .build());
    }

    private void sendTemplate(String to, String subject, EmailTemplate template) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new EmailDeliveryException("SMTP mail sender is not configured. Missing MAIL_USERNAME.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildEmailTemplate(template), true);
            addLogoInline(helper);
            mailSender.send(message);
            log.info("HTML email sent to {} with subject '{}'", to, subject);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send HTML email to {} with subject '{}': {}", to, subject, ex.getMessage());
            throw new EmailDeliveryException("Impossible d envoyer l e-mail pour le moment.", ex);
        }
    }

    private void addLogoInline(MimeMessageHelper helper) throws MessagingException {
        ClassPathResource logo = new ClassPathResource(LOGO_PATH);
        if (logo.exists()) {
            helper.addInline(LOGO_CID, logo);
        } else {
            log.warn("Mail logo not found at classpath:{}", LOGO_PATH);
        }
    }

    private String buildEmailTemplate(EmailTemplate template) {
        String logoBlock = """
                <img src="cid:%s" width="110" alt="Attijari Compass" style="display:block;border:0;outline:none;text-decoration:none;margin:0 auto 14px;" />
                """.formatted(LOGO_CID);
        String codeBlock = StringUtils.hasText(template.verificationCode())
                ? """
                  <div style="margin:24px auto;padding:18px 20px;background:#fff4ec;border:1px solid #F36F21;border-radius:14px;text-align:center;max-width:280px;">
                    <div style="font-size:30px;line-height:1.2;font-weight:800;letter-spacing:8px;color:#F36F21;">%s</div>
                  </div>
                  """.formatted(escapeHtml(template.verificationCode()))
                : "";
        String buttonBlock = StringUtils.hasText(template.buttonText()) && StringUtils.hasText(template.buttonUrl())
                ? """
                  <div style="text-align:center;margin:28px 0 10px;">
                    <a href="%s" style="display:inline-block;background:#F36F21;color:#ffffff;text-decoration:none;font-weight:800;padding:13px 22px;border-radius:999px;">%s</a>
                  </div>
                  """.formatted(escapeHtml(template.buttonUrl()), escapeHtml(template.buttonText()))
                : "";
        String warningBlock = StringUtils.hasText(template.warningMessage())
                ? """
                  <div style="margin-top:22px;padding:14px 16px;background:#fff8f2;border-left:4px solid #F36F21;border-radius:10px;color:#7a4216;font-size:14px;line-height:1.6;">%s</div>
                  """.formatted(escapeHtml(template.warningMessage()))
                : "";

        return """
                <!doctype html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Attijari Compass</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,Helvetica,sans-serif;color:#111827;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f6f9;padding:28px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:18px;overflow:hidden;box-shadow:0 18px 48px rgba(17,24,39,0.10);">
                          <tr>
                            <td style="background:#F36F21;padding:24px 24px 18px;text-align:center;">
                              %s
                              <div style="color:#fff;font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;">Attijari Compass</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px 32px 28px;">
                              <p style="margin:0 0 12px;color:#6b7280;font-size:14px;">Bonjour %s,</p>
                              <h1 style="margin:0;color:#111827;font-size:24px;line-height:1.25;">%s</h1>
                              <p style="margin:10px 0 0;color:#F36F21;font-size:15px;font-weight:700;line-height:1.5;">%s</p>
                              <p style="margin:20px 0 0;color:#374151;font-size:15px;line-height:1.7;">%s</p>
                              %s
                              %s
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 28px;background:#111111;color:#d1d5db;text-align:center;font-size:12px;line-height:1.6;">
                              © 2026 Attijari Compass. Votre banque intelligente.<br>
                              Si vous n’êtes pas à l’origine de cette demande, vous pouvez ignorer cet e-mail.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                logoBlock,
                escapeHtml(template.fullName()),
                escapeHtml(template.title()),
                escapeHtml(template.subtitle()),
                escapeHtml(template.mainMessage()),
                codeBlock,
                buttonBlock,
                warningBlock
        );
    }

    private String loginUrl() {
        return frontendUrl.replaceAll("/+$", "") + "/login";
    }

    private String displayName(String fullName) {
        return StringUtils.hasText(fullName) ? fullName.trim() : "Utilisateur";
    }

    private String safeReason(String reason) {
        return StringUtils.hasText(reason) ? reason.trim() : "Non précisé";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Builder
    private record EmailTemplate(
            String fullName,
            String title,
            String subtitle,
            String mainMessage,
            String buttonText,
            String buttonUrl,
            String verificationCode,
            String warningMessage
    ) {
    }
}
