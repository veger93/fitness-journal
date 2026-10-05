package com.vegas.workout.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Топик объявляет его "владелец" — тот, кто в него пишет.
 * При старте Spring (KafkaAdmin) создаст топик, если его ещё нет.
 *
 * 3 партиции: сообщения с одинаковым ключом (userId) всегда попадают в одну партицию
 * и читаются строго по порядку; разные пользователи обрабатываются параллельно.
 * replicas(1): у нас один брокер. В проде — 3.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic workoutEventsTopic(@Value("${app.kafka.topics.workout-events}") String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
