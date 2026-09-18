package tz.elmkusoma.workers.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public DirectExchange elmkusomaExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
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
    public Binding notificationBinding(Queue notificationQueue, DirectExchange elmkusomaExchange) {
        return BindingBuilder.bind(notificationQueue).to(elmkusomaExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding reportBinding(Queue reportQueue, DirectExchange elmkusomaExchange) {
        return BindingBuilder.bind(reportQueue).to(elmkusomaExchange).with(REPORT_ROUTING_KEY);
    }

    @Bean
    public Binding certificateBinding(Queue certificateQueue, DirectExchange elmkusomaExchange) {
        return BindingBuilder.bind(certificateQueue).to(elmkusomaExchange).with(CERTIFICATE_ROUTING_KEY);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, DirectExchange elmkusomaExchange) {
        return BindingBuilder.bind(emailQueue).to(elmkusomaExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
