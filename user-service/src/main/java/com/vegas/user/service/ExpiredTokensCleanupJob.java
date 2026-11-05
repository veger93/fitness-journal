package com.vegas.user.service;

import com.vegas.user.repository.ClaimTokenRepository;
import com.vegas.user.repository.EmailCodeRepository;
import com.vegas.user.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Уборка истёкших "пропусков" раз в сутки: refresh-токены, ссылки передачи аккаунта (QR), коды из писем.
 * Без неё таблицы растут бесконечно: каждый вход и каждое обновление токена (раз в 15 минут на устройство)
 * добавляет строку в refresh_tokens.
 *
 * Удаляем только то, что истекло больше недели назад (retention), а не сразу:
 * - refresh: отозванный при ротации токен нужен, чтобы заметить кражу (его предъявили повторно —
 *   отзываем всю сессию). Пока токен не истёк, он должен лежать; после истечения он бесполезен и вору;
 * - неделя запаса — чтобы в логах и при разборе проблемы было видно, что происходило в последние дни.
 * Удаление — одним DELETE на таблицу (без загрузки строк в память), всё в одной транзакции.
 */
@Slf4j
@Component
public class ExpiredTokensCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final ClaimTokenRepository claimTokenRepository;
    private final EmailCodeRepository emailCodeRepository;
    private final Clock clock;
    private final Duration retention;

    public ExpiredTokensCleanupJob(RefreshTokenRepository refreshTokenRepository, ClaimTokenRepository claimTokenRepository,
                                   EmailCodeRepository emailCodeRepository, Clock clock,
                                   @Value("${app.cleanup.tokens-retention:P7D}") Duration retention) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.claimTokenRepository = claimTokenRepository;
        this.emailCodeRepository = emailCodeRepository;
        this.clock = clock;
        this.retention = retention;
    }

    // ISO-8601: PT24H = раз в сутки; первый запуск через 5 минут после старта (не мешаем прогреву)
    @Scheduled(fixedDelayString = "${app.cleanup.tokens-interval:PT24H}",
            initialDelayString = "${app.cleanup.tokens-initial-delay:PT5M}")
    @Transactional
    public void deleteExpired() {
        Instant cutoff = clock.instant().minus(retention);
        int refresh = refreshTokenRepository.deleteExpiredBefore(cutoff);
        int claims = claimTokenRepository.deleteExpiredBefore(cutoff);
        int codes = emailCodeRepository.deleteExpiredBefore(cutoff);
        if (refresh + claims + codes > 0) {
            log.info("Expired tokens removed: refresh={}, claim={}, emailCodes={}", refresh, claims, codes);
        }
    }
}
