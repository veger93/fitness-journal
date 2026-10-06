package com.vegas.user.event;

import com.vegas.common.event.EventTypes;
import com.vegas.common.event.UserProfileUpdatedEvent;
import com.vegas.common.event.UserRegisteredEvent;
import com.vegas.common.exception.NotFoundException;
import com.vegas.outbox.OutboxWriter;
import com.vegas.user.entity.BodyWeightEntry;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.repository.BodyWeightEntryRepository;
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
 * Какие события пользователя пишутся в outbox (саму запись делает OutboxWriter из common-outbox).
 * Зависит только от репозиториев (не от сервисов), поэтому его можно звать и из ProfileService,
 * и из BodyWeightService, и из AuthService без циклических зависимостей.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxWriter outboxWriter;
    private final UserProfileRepository profileRepository;
    private final BodyWeightEntryRepository weightRepository;
    private final Clock clock;

    /** Для notification-service: куда слать письма. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void userRegistered(User user) {
        UserRegisteredEvent event = new UserRegisteredEvent(user.getId(), user.getEmail(), user.getDisplayName(), clock.instant());
        outboxWriter.write(user.getId(), EventTypes.USER_REGISTERED, user.getId().toString(), event);
    }

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
        outboxWriter.write(userId, EventTypes.USER_PROFILE_UPDATED, userId.toString(), event);
    }
}
