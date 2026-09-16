package tz.elmkusoma.realtime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter notificationListenerAdapter,
            MessageListenerAdapter presenceListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationListenerAdapter, new ChannelTopic("notifications"));
        container.addMessageListener(presenceListenerAdapter, new ChannelTopic("presence"));
        return container;
    }

    @Bean
    public MessageListenerAdapter notificationListenerAdapter() {
        return new MessageListenerAdapter(new NotificationMessageListener(), "onMessage");
    }

    @Bean
    public MessageListenerAdapter presenceListenerAdapter() {
        return new MessageListenerAdapter(new PresenceMessageListener(), "onMessage");
    }

    public static class NotificationMessageListener implements org.springframework.data.redis.connection.MessageListener {
        @Override
        public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
            // Handled by RabbitMQ listener primarily
        }
    }

    public static class PresenceMessageListener implements org.springframework.data.redis.connection.MessageListener {
        @Override
        public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
            // Handled by RabbitMQ listener primarily
        }
    }
}
