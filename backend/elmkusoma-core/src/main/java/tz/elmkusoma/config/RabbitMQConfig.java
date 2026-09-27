package tz.elmkusoma.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageHeaders;
import org.springframework.amqp.core.MessagePostProcessor;
import tz.elmkusoma.config.security.OrganizationContext;
import tz.elmkusoma.config.security.OrganizationContextHolder;

import java.util.UUID;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "elmkusoma.exchange";
    public static final String NOTIFICATION_QUEUE = "elmkusoma.notification.queue";
    public static final String REPORT_QUEUE = "elmkusoma.report.queue";
    public static final String CERTIFICATE_QUEUE = "elmkusoma.certificate.queue";
    public static final String EMAIL_QUEUE = "elmkusoma.email.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "elmkusoma.notification";
    public static final String REPORT_ROUTING_KEY = "elmkusoma.report";
    public static final String CERTIFICATE_ROUTING_KEY = "elmkusoma.certificate";
    public static final String EMAIL_ROUTING_KEY = "elmkusoma.email";
    private static final String HEADER_INSTITUTION_ID = "x-institution-id";

    @Bean
    public TopicExchange elmkusomaExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue reportQueue() {
        return new Queue(REPORT_QUEUE, true);
    }

    @Bean
    public Queue certificateQueue() {
        return new Queue(CERTIFICATE_QUEUE, true);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(notificationQueue).to(elmkusomaExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding reportBinding(Queue reportQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(reportQueue).to(elmkusomaExchange).with(REPORT_ROUTING_KEY);
    }

    @Bean
    public Binding certificateBinding(Queue certificateQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(certificateQueue).to(elmkusomaExchange).with(CERTIFICATE_ROUTING_KEY);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(emailQueue).to(elmkusomaExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public MessagePostProcessor institutionIdHeaderPostProcessor(OrganizationContextHolder contextHolder) {
        return message -> {
            UUID institutionId = null;
            OrganizationContext context = contextHolder.getContext();
            if (context != null) {
                institutionId = context.getInstitutionId();
            }
            if (institutionId != null) {
                message.getMessageProperties().getHeaders().put(HEADER_INSTITUTION_ID, institutionId.toString());
            }
            return message;
        };
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter jsonMessageConverter,
                                          MessagePostProcessor institutionIdHeaderPostProcessor) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        rabbitTemplate.setBeforePublishPostProcessors(institutionIdHeaderPostProcessor);
        return rabbitTemplate;
    }
}
