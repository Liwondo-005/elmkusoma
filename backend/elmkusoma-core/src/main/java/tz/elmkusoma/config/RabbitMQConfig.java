package tz.elmkusoma.config;

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

    public static final String QUEUE_NOTIFICATIONS = "elmkusoma.notifications";
    public static final String QUEUE_REPORTS = "elmkusoma.reports";
    public static final String QUEUE_CERTIFICATES = "elmkusoma.certificates";

    public static final String ROUTING_KEY_NOTIFICATIONS = "notification";
    public static final String ROUTING_KEY_REPORTS = "report";
    public static final String ROUTING_KEY_CERTIFICATES = "certificate";

    @Bean
    public DirectExchange elmkusomaExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationsQueue() {
        return new Queue(QUEUE_NOTIFICATIONS, true);
    }

    @Bean
    public Queue reportsQueue() {
        return new Queue(QUEUE_REPORTS, true);
    }

    @Bean
    public Queue certificatesQueue() {
        return new Queue(QUEUE_CERTIFICATES, true);
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, DirectExchange elmkusomaExchange) {
        return BindingBuilder.bind(notificationsQueue).to(elmkusomaExchange()).with(ROUTING_KEY_NOTIFICATIONS);
    }

    @Bean
    public Binding reportsBinding(Queue reportsQueue, DirectExchange elmkusomaExchange) {
        return BindingBuilder.bind(reportsQueue).to(elmkusomaExchange()).with(ROUTING_KEY_REPORTS);
    }

    @Bean
    public Binding certificatesBinding(Queue certificatesQueue, DirectExchange elmkusomaExchange) {
        return BindingBuilder.bind(certificatesQueue).to(elmkusomaExchange()).with(ROUTING_KEY_CERTIFICATES);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
