package tz.elmkusoma.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "elmkusoma.exchange";
    public static final String NOTIFICATION_QUEUE = "elmkusoma.notification.queue";
    public static final String REPORT_QUEUE = "elmkusoma.report.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "elmkusoma.notification";
    public static final String REPORT_ROUTING_KEY = "elmkusoma.report";

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
    public Binding notificationBinding(Queue notificationQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(notificationQueue).to(elmkusomaExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding reportBinding(Queue reportQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(reportQueue).to(elmkusomaExchange).with(REPORT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
