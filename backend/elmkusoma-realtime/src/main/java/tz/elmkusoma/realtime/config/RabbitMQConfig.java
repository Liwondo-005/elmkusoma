package tz.elmkusoma.realtime.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "elmkusoma.exchange";
    public static final String NOTIFICATION_QUEUE = "elmkusoma.realtime.notification.queue";
    public static final String PRESENCE_QUEUE = "elmkusoma.realtime.presence.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "elmkusoma.notification.realtime";
    public static final String PRESENCE_ROUTING_KEY = "elmkusoma.presence.realtime";

    @Bean
    public TopicExchange elmkusomaExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue presenceQueue() {
        return new Queue(PRESENCE_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(notificationQueue).to(elmkusomaExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding presenceBinding(Queue presenceQueue, TopicExchange elmkusomaExchange) {
        return BindingBuilder.bind(presenceQueue).to(elmkusomaExchange).with(PRESENCE_ROUTING_KEY);
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
