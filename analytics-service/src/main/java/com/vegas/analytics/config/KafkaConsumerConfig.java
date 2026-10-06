package com.vegas.analytics.config;

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
 * Что делать, если сообщение не удалось обработать.
 *
 * 1) Повторить 3 раза с паузой 1 сек (временные сбои: БД моргнула и т.п.).
 * 2) Не получилось — переложить сообщение в Dead Letter Topic "workout-events.DLT"
 *    и идти дальше. Без этого одно "битое" сообщение навсегда остановило бы обработку
 *    партиции: consumer пытался бы прочитать его снова и снова.
 * Сообщения из DLT можно посмотреть в Kafka UI, разобраться и переотправить.
 */
@Configuration
public class KafkaConsumerConfig {

    /** DLT: по умолчанию сообщение кладётся в ту же партицию, что и в исходном топике -> партиций столько же. */
    @Bean
    public NewTopic workoutEventsDeadLetterTopic(@Value("${app.kafka.topics.workout-events}") String topic) {
        return TopicBuilder.name(topic + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userEventsDeadLetterTopic(@Value("${app.kafka.topics.user-events}") String topic) {
        return TopicBuilder.name(topic + ".DLT")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /** Spring Boot сам подключит этот бин ко всем @KafkaListener. */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
        // кривой JSON не исправится от повторов — сразу в DLT
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
