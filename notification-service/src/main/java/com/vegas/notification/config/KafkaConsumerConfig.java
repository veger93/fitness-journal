package com.vegas.notification.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Ошибки обработки: 3 повтора с паузой (почтовый сервер мог моргнуть), затем DLT.
 * user-events.DLT уже объявлен в analytics-service — объявление топика идемпотентно, дубль безвреден.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public NewTopic analyticsEventsDeadLetterTopic(@Value("${app.kafka.topics.analytics-events}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic userEventsDeadLetterTopic(@Value("${app.kafka.topics.user-events}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(2_000L, 3));
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
