package com.vegas.user.service;

import com.vegas.user.entity.User;
import com.vegas.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Удаляет истёкшие демо (docs/demo.md) — раз в 10 минут, пачками по 50.
 *
 * Не сразу после demo_expires_at, а через grace (1 час): последний access-токен демо живёт 15 минут.
 * Удалить раньше — запрос с ещё живым токеном создал бы тренировку уже удалённого пользователя ("сироту").
 *
 * Каждое демо — отдельной транзакцией (AccountDeletionService.deleteExpiredDemo): ошибка в одном
 * не мешает удалить остальные; не удалилось — попробуем в следующий раз.
 */
@Slf4j
@Component
public class DemoCleanupJob {

    private final UserRepository userRepository;
    private final AccountDeletionService deletionService;
    private final Clock clock;
    private final Duration grace;

    public DemoCleanupJob(UserRepository userRepository, AccountDeletionService deletionService, Clock clock,
                          @Value("${app.demo.delete-grace:1h}") Duration grace) {
        this.userRepository = userRepository;
        this.deletionService = deletionService;
        this.clock = clock;
        this.grace = grace;
    }

    // ISO-8601: PT10M = 10 минут (короткая запись "10m" в @Scheduled не поддерживается)
    @Scheduled(fixedDelayString = "${app.demo.cleanup-interval:PT10M}", initialDelayString = "PT1M")
    public void deleteExpired() {
        Instant cutoff = clock.instant().minus(grace);
        // по провайдеру: DEMO раньше NONE (по алфавиту) — сначала демо-тренеры, их клиенты удалятся вместе с ними
        List<User> expired = userRepository.findTop50ByDemoExpiresAtBeforeOrderByAuthProviderAsc(cutoff);
        for (User user : expired) {
            try {
                deletionService.deleteExpiredDemo(user.getId());
            } catch (RuntimeException e) {
                log.warn("Demo user {} not deleted, will retry: {}", user.getId(), e.getMessage());
            }
        }
        if (!expired.isEmpty()) {
            log.info("Expired demos processed: {}", expired.size());
        }
    }
}
