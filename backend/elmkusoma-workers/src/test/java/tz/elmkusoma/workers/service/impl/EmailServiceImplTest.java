package tz.elmkusoma.workers.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Behaviour of the worker's outbound email, with the transport mocked.
 *
 * <p>Nothing here opens an SMTP session: {@link JavaMailSender} is a mock, so these
 * tests assert what the application decides (who sends, what body, what happens on
 * failure) without contacting Gmail or any external service.</p>
 */
class EmailServiceImplTest {

    private static final String CONFIGURED_SENDER = "configured.sender@example.test";
    private static final String RECIPIENT = "learner@example.test";

    private JavaMailSender mailSender;
    private TemplateEngine templateEngine;
    private EmailServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        MimeMessage message = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        service = new EmailServiceImpl(mailSender, templateEngine);
        // The sender arrives via @Value in the real context.
        ReflectionTestUtils.setField(service, "fromAddress", CONFIGURED_SENDER);
    }

    @Test
    @DisplayName("The envelope sender comes from configuration, not a hardcoded address")
    void sendsUsingTheConfiguredSender() throws Exception {
        service.sendEmail(RECIPIENT, "Subject", "<p>Body</p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertEquals(CONFIGURED_SENDER, sent.getFrom()[0].toString(),
                "the hardcoded address could not authenticate from a Gmail login");
        assertEquals(RECIPIENT, sent.getAllRecipients()[0].toString());
        assertEquals("Subject", sent.getSubject());
    }

    @Test
    @DisplayName("A templated email renders the named template and honours the recipient")
    void rendersTheRequestedTemplate() throws Exception {
        when(templateEngine.process(any(String.class), any())).thenReturn("<p>rendered</p>");

        service.sendTemplatedEmail(RECIPIENT, "Welcome", "welcome", Map.of("name", "Amina"));

        verify(templateEngine).process(any(String.class), any());
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals(RECIPIENT, captor.getValue().getAllRecipients()[0].toString());
    }

    @Test
    @DisplayName("An SMTP failure surfaces so the queue listener can retry it")
    void smtpFailurePropagatesInsteadOfBeingSwallowed() {
        doThrow(new MailSendException("smtp unavailable")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendException.class,
                () -> service.sendEmail(RECIPIENT, "Subject", "<p>Body</p>"),
                "a swallowed failure would mark undelivered mail as sent");
    }

    @Test
    @DisplayName("A template that cannot be rendered is reported, not delivered blank")
    void templateFailurePropagates() {
        when(templateEngine.process(any(String.class), any()))
                .thenThrow(new RuntimeException("template missing"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.sendTemplatedEmail(RECIPIENT, "S", "missing-template", Map.of()));
        assertTrue(ex.getMessage().contains("Failed to send templated email"));
    }
}