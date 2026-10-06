package com.vegas.user.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.event.EventTypes;
import com.vegas.common.event.UserProfileUpdatedEvent;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.entity.BodyWeightEntry;
import com.vegas.user.entity.OutboxEvent;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.repository.BodyWeightEntryRepository;
import com.vegas.user.repository.OutboxEventRepository;
import com.vegas.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Записывает события пользователя в outbox-таблицу — в той же транзакции, что и изменение данных.
 * Зависит только от репозиториев (не от сервисов), поэтому его можно звать и из ProfileService,
 * и из BodyWeightService без циклических зависимостей.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final UserProfileRepository profileRepository;
    private final BodyWeightEntryRepository weightRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Снимок профиля + текущий вес.
     * Запрос веса ниже увидит только что сохранённую запись: перед JPQL-запросом Hibernate
     * сам сбрасывает (flush) несохранённые изменения затронутых таблиц.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void profileUpdated(UUID userId) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Профиль не найден"));
        BigDecimal currentWeight = weightRepository.findFirstByUserIdOrderByMeasuredOnDesc(userId)
                .map(BodyWeightEntry::getWeightKg)
                .orElse(null);
        Instant now = clock.instant();

        UserProfileUpdatedEvent event = new UserProfileUpdatedEvent(
                userId,
                profile.getGender(),
                profile.getBirthYear() == null ? null : profile.getBirthYear().intValue(),
                profile.getHeightCm() == null ? null : profile.getHeightCm().intValue(),
                profile.getExperienceLevel() == null ? null : profile.getExperienceLevel().name(),
                currentWeight,
                now
        );
        outboxRepository.save(OutboxEvent.create(userId, EventTypes.USER_PROFILE_UPDATED, userId.toString(), toJson(event), now));
    }

    private String toJson(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + event.getClass().getSimpleName(), e);
        }
    }
}
