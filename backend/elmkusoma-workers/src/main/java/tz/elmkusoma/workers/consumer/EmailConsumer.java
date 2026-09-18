package tz.elmkusoma.workers.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import tz.elmkusoma.workers.config.RabbitMQConfig;
import tz.elmkusoma.workers.event.EmailEvent;
import tz.elmkusoma.workers.service.EmailService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleEmailEvent(EmailEvent event) {
        log.info("Received email event: id={}, to={}, template={}",
                event.getId(), event.getToEmail(), event.getTemplateName());

        try {
            if (event.getTemplateName() != null && !event.getTemplateName().isEmpty()) {
                emailService.sendTemplatedEmail(
                        event.getToEmail(),
                        event.getSubject(),
                        event.getTemplateName(),
                        event.getTemplateVariables()
                );
            } else {
                String htmlContent = buildDefaultHtmlContent(event);
                emailService.sendEmail(
                        event.getToEmail(),
                        event.getSubject(),
                        htmlContent
                );
            }

            log.info("Email sent successfully to: {} for event id: {}",
                    event.getToEmail(), event.getId());

        } catch (Exception e) {
            log.error("Failed to send email to: {} for event id: {}. Attempt will be retried. Error: {}",
                    event.getToEmail(), event.getId(), e.getMessage(), e);
            throw e;
        }
    }

    private String buildDefaultHtmlContent(EmailEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h2>").append(event.getSubject()).append("</h2>");
        sb.append("<p>This is an automated notification from Elmkusoma LMS.</p>");

        if (event.getTemplateVariables() != null && !event.getTemplateVariables().isEmpty()) {
            sb.append("<div>");
            event.getTemplateVariables().forEach((key, value) ->
                sb.append("<p><strong>").append(key).append(":</strong> ").append(value).append("</p>")
            );
            sb.append("</div>");
        }

        sb.append("<br/><p>Best regards,<br/>Elmkusoma Team</p>");
        sb.append("</body></html>");

        return sb.toString();
    }
}
