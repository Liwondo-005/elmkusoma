package tz.elmkusoma.identity.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OtpMailService contract: real SMTP hand-off with the code in the body,
 * graceful no-op when SMTP is unconfigured, and never-throw delivery.
 */
class OtpMailServiceTest {

    private static MimeMessage newMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> providerWith(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    private static OtpMailService service(String mailHost, JavaMailSender sender) {
        OtpMailService service = new OtpMailService(providerWith(sender));
        reflectSet(service, "mailHost", mailHost);
        reflectSet(service, "fromAddress", "noreply@elmkusoma.co.tz");
        return service;
    }

    private static void reflectSet(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void configuredSender_receivesMessageWithCodeInBody() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(newMessage());

        service("localhost", sender).sendVerificationCode("learner@example.com", "04217");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, times(1)).send(captor.capture());

        MimeMessage sent = captor.getValue();
        assertEquals("learner@example.com", sent.getAllRecipients()[0].toString());
        assertEquals("Your ELMKUSOMA verification code", sent.getSubject());
        assertTrue(sent.getContent().toString().contains("04217"),
                "the code must be in the mail body");
        assertTrue(sent.getContent().toString().contains("10 minutes"),
                "the expiry must be stated");
    }

    @Test
    void unconfiguredHost_neverSends() {
        JavaMailSender sender = mock(JavaMailSender.class);

        service("", sender).sendVerificationCode("learner@example.com", "04217");

        verify(sender, never()).createMimeMessage();
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void smtpFailure_isSwallowed_andNeverThrows() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(newMessage());
        doThrow(new MailSendException("connection refused")).when(sender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> service("localhost", sender)
                .sendVerificationCode("learner@example.com", "04217"));
    }
}
