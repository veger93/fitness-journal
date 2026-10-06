package com.vegas.user.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Топик событий пользователя. Ключ сообщения = userId -> события одного пользователя по порядку. */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userEventsTopic(@Value("${app.kafka.topics.user-events}") String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
