package tz.elmkusoma.workers.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import tz.elmkusoma.workers.service.EmailService;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * Envelope sender, taken from configuration rather than hardcoded.
     *
     * <p>This mirrors core's {@code OtpMailService}, which already reads the same
     * {@code elmkusoma.mail.from} key, so both modules now honour one property and
     * {@code ELMKUSOMA_MAIL_FROM} overrides it for both.</p>
     *
     * <p>The previous literal could not be used for local delivery at all: a personal
     * Gmail login may only reliably send as the account it authenticated as, so a
     * hardcoded {@code noreply@elmkusoma.co.tz} fails SPF/DMARC in development. In
     * production the default keeps the existing branded behaviour unchanged.</p>
     */
    @Value("${elmkusoma.mail.from:noreply@elmkusoma.co.tz}")
    private String fromAddress;

    @Override
    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async
    public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            sendEmail(to, subject, htmlContent);
            log.info("Templated email sent successfully to: {} with template: {}", to, templateName);
        } catch (Exception e) {
            log.error("Failed to send templated email to: {} with template: {}. Error: {}",
                    to, templateName, e.getMessage(), e);
            throw new RuntimeException("Failed to send templated email", e);
        }
    }
}
