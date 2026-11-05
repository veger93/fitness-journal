package com.vegas.user.service;

import com.vegas.user.repository.ClaimTokenRepository;
import com.vegas.user.repository.EmailCodeRepository;
import com.vegas.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpiredTokensCleanupJobTest {

    private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private ClaimTokenRepository claimTokenRepository;
    @Mock
    private EmailCodeRepository emailCodeRepository;

    @Test
    void deletesOnlyExpiredMoreThanRetentionAgo_inAllThreeTables() {
        ExpiredTokensCleanupJob job = new ExpiredTokensCleanupJob(refreshTokenRepository, claimTokenRepository,
                emailCodeRepository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(7));

        job.deleteExpired();

        // истёкшие больше недели назад; свежеистёкшие refresh-токены ещё нужны для обнаружения кражи
        Instant cutoff = Instant.parse("2026-10-29T03:00:00Z");
        verify(refreshTokenRepository).deleteExpiredBefore(cutoff);
        verify(claimTokenRepository).deleteExpiredBefore(cutoff);
        verify(emailCodeRepository).deleteExpiredBefore(cutoff);
    }
}
