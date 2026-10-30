package com.vegas.traineraccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.TrainerClientLinkedEvent;
import com.vegas.common.event.TrainerClientUnlinkedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

/**
 * Обновляет копию связей "тренер — клиент" по событиям из user-service.
 *
 * Своя consumer group: "<имя сервиса>.trainer-links". Если бы слушатель был в общей группе сервиса,
 * а у сервиса уже есть другой слушатель user-events (analytics), Kafka поделила бы партиции между ними —
 * и каждый видел бы только часть событий. Отдельная группа получает ВСЕ сообщения топика.
 */
@Slf4j
@RequiredArgsConstructor
public class TrainerLinkListener {

    private final TrainerLinkRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    @KafkaListener(topics = "${app.kafka.topics.user-events:user-events}",
            groupId = "${spring.application.name}.trainer-links",
            // новая группа читает топик с начала: связи, созданные до запуска сервиса, тоже попадут в копию
            properties = "auto.offset.reset=earliest")
    public void onUserEvent(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Header header = record.headers().lastHeader(EventTypes.HEADER);
        String eventType = header == null ? null : new String(header.value(), StandardCharsets.UTF_8);

        if (EventTypes.TRAINER_CLIENT_LINKED.equals(eventType)) {
            TrainerClientLinkedEvent event = objectMapper.readValue(record.value(), TrainerClientLinkedEvent.class);
            // save по первичному ключу: повтор того же события (Kafka доставляет "минимум один раз") ничего не испортит
            repository.save(new TrainerLink(event.clientId(), event.trainerId(), event.linkedAt(), event.clientName()));
            log.info("Trainer {} linked to client {}", event.trainerId(), event.clientId());
        } else if (EventTypes.TRAINER_CLIENT_UNLINKED.equals(eventType)) {
            TrainerClientUnlinkedEvent event = objectMapper.readValue(record.value(), TrainerClientUnlinkedEvent.class);
            // удаляем, только если в копии всё ещё ЭТОТ тренер: повтор старого события не снесёт новую связь
            repository.findById(event.clientId())
                    .filter(link -> link.getTrainerId().equals(event.trainerId()))
                    .ifPresent(repository::delete);
            log.info("Trainer {} unlinked from client {}", event.trainerId(), event.clientId());
        }
    }
}
