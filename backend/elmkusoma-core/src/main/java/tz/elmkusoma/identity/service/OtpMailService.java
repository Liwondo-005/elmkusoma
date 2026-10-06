package tz.elmkusoma.identity.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Delivers OTP codes over SMTP when {@code spring.mail.host} is configured.
 *
 * <p>Delivery is deliberately best-effort: the code is already persisted in
 * {@code verification_codes} (and logged server-side) before this runs, so an
 * SMTP outage must never break the {@code /v1/auth/send-code} contract — the
 * failure is logged loudly instead, and the cooldown/resend UX lets the user
 * retry.</p>
 */
@Service
public class OtpMailService {

    private static final Logger log = LoggerFactory.getLogger(OtpMailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${elmkusoma.mail.from:noreply@elmkusoma.co.tz}")
    private String fromAddress;

    public OtpMailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    /**
     * Sends the 5-digit code to the given address. Never throws.
     */
    public void sendVerificationCode(String email, String code) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || mailHost == null || mailHost.isBlank()) {
            log.info("OTP email delivery not configured (spring.mail.host empty); "
                    + "code remains available in verification_codes for {}", email);
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_NO, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("Your ELMKUSOMA verification code");
            helper.setText(buildBody(code), false);
            sender.send(message);
            log.info("Verification code emailed to {}", email);
        } catch (MailException | MessagingException ex) {
            // Best-effort: the send-code request must still succeed.
            log.error("OTP email delivery to {} failed: {}", email, ex.getMessage());
        }
    }

    private static String buildBody(String code) {
        return ("""
                Your ELMKUSOMA verification code is: %s

                The code expires in 10 minutes. Anyone who has it can verify your account — never share it with anyone.

                Msimbo wako wa ELMKUSOMA ni: %s
                Msimbo unaisha baada ya dakika 10. Usiambie mtu yeyote.
                """).formatted(code, code);
    }
}
