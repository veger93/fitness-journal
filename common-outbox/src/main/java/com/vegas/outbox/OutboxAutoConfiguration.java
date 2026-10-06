package com.vegas.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Auto-configuration: так устроены все Spring Boot стартеры.
 * Spring Boot находит этот класс через файл
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * и создаёт бины outbox в любом сервисе, где есть зависимость на common-outbox.
 *
 * Тонкость: сервис сканирует только свой пакет (com.vegas.workout и т.п.), а сущность
 * OutboxEvent и её репозиторий лежат в com.vegas.outbox. Registrar ниже ДОБАВЛЯЕТ этот пакет
 * к списку, который Spring Boot сканирует на @Entity и JPA-репозитории.
 * (@EntityScan здесь не подходит: он бы ЗАМЕНИЛ пакеты сервиса, и его сущности пропали бы.)
 * Поэтому конфигурация обрабатывается раньше JPA-автоконфигураций (before = ...).
 */
@AutoConfiguration(before = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
@Import(OutboxAutoConfiguration.OutboxPackageRegistrar.class)
public class OutboxAutoConfiguration {

    @Bean
    public OutboxWriter outboxWriter(OutboxEventRepository outboxRepository, ObjectMapper objectMapper,
                                     ObjectProvider<Clock> clock) {
        return new OutboxWriter(outboxRepository, objectMapper, clock.getIfAvailable(Clock::systemUTC));
    }

    @Bean
    public OutboxRelay outboxRelay(OutboxEventRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectProvider<Clock> clock, OutboxProperties properties) {
        return new OutboxRelay(outboxRepository, kafkaTemplate, clock.getIfAvailable(Clock::systemUTC), properties);
    }

    @Bean
    public OutboxPublisher outboxPublisher(OutboxRelay outboxRelay) {
        return new OutboxPublisher(outboxRelay);
    }

    static class OutboxPackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, OutboxEvent.class.getPackageName());
        }
    }
}
