package com.vegas.traineraccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Auto-configuration модуля (как OutboxAutoConfiguration): регистрирует пакет с TrainerLink для JPA
 * и создаёт слушатель Kafka. Фильтр сервис создаёт сам в SecurityConfig — см. TrainerAccessFilter.
 */
@AutoConfiguration(before = {HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
@Import(TrainerAccessAutoConfiguration.PackageRegistrar.class)
public class TrainerAccessAutoConfiguration {

    @Bean
    public TrainerLinkListener trainerLinkListener(TrainerLinkRepository repository, ObjectMapper objectMapper) {
        return new TrainerLinkListener(repository, objectMapper);
    }

    static class PackageRegistrar implements ImportBeanDefinitionRegistrar {
        @Override
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, TrainerLink.class.getPackageName());
        }
    }
}
