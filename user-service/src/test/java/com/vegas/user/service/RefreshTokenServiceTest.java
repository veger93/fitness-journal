package com.vegas.user.service;

import com.vegas.user.config.JwtProperties;
import com.vegas.user.entity.RefreshToken;
import com.vegas.user.entity.RefreshToken.RevokeReason;
import com.vegas.user.exception.InvalidRefreshTokenException;
import com.vegas.user.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    private final Instant now = Instant.parse("2026-10-28T10:00:00Z");
    private final UUID userId = UUID.randomUUID();
    private final UUID familyId = UUID.randomUUID();

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties("secret", Duration.ofMinutes(15), Duration.ofDays(30), false);
        service = new RefreshTokenService(repository, properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void issue_storesOnlyHashAndExpiresInThirtyDays() {
        String raw = service.issueNewFamily(userId);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(now.plus(Duration.ofDays(30)));
        assertThat(raw).hasSizeGreaterThanOrEqualTo(43); // 32 байта в base64url
    }

    @Test
    void rotate_revokesOldAndIssuesNewInSameFamily() {
        RefreshToken old = token(now.plus(Duration.ofDays(1)));
        when(repository.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(old));

        RefreshTokenService.Rotation rotation = service.rotate("old");

        assertThat(old.getRevokedReason()).isEqualTo(RevokeReason.ROTATED);
        assertThat(rotation.userId()).isEqualTo(userId);
        assertThat(rotation.refreshToken()).isNotEqualTo("old");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(familyId);
    }

    @Test
    void rotate_reusedTokenAfterGrace_revokesWholeFamily() {
        RefreshToken old = token(now.plus(Duration.ofDays(1)));
        old.revoke(RevokeReason.ROTATED, now.minus(Duration.ofMinutes(5)));
        when(repository.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.rotate("old")).isInstanceOf(InvalidRefreshTokenException.class);
        // копия токена у кого-то ещё — отзываем все токены этого входа
        verify(repository).revokeFamily(familyId, RevokeReason.REUSE, now);
    }

    @Test
    void rotate_reusedWithinGrace_isTreatedAsParallelRequest() {
        RefreshToken old = token(now.plus(Duration.ofDays(1)));
        old.revoke(RevokeReason.ROTATED, now.minusSeconds(5));
        when(repository.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(old));
        when(repository.existsByFamilyIdAndRevokedAtIsNull(familyId)).thenReturn(true);

        RefreshTokenService.Rotation rotation = service.rotate("old");

        assertThat(rotation.userId()).isEqualTo(userId);
        verify(repository, never()).revokeFamily(any(), any(), any());
    }

    @Test
    void rotate_withinGraceButAfterLogout_isInvalid() {
        // токен обменяли, а потом с нового вышли — старая копия не должна "воскресить" вход
        RefreshToken old = token(now.plus(Duration.ofDays(1)));
        old.revoke(RevokeReason.ROTATED, now.minusSeconds(5));
        when(repository.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(old));
        when(repository.existsByFamilyIdAndRevokedAtIsNull(familyId)).thenReturn(false);

        assertThatThrownBy(() -> service.rotate("old")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).save(any());
        verify(repository, never()).revokeFamily(any(), any(), any());
    }

    @Test
    void rotate_afterLogout_isInvalidWithoutAlarm() {
        RefreshToken old = token(now.plus(Duration.ofDays(1)));
        old.revoke(RevokeReason.LOGOUT, now.minusSeconds(5));
        when(repository.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service.rotate("old")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).revokeFamily(any(), any(), any());
    }

    @Test
    void rotate_expiredToken_isInvalid() {
        when(repository.findByTokenHash(RefreshTokenService.hash("old"))).thenReturn(Optional.of(token(now.minusSeconds(1))));

        assertThatThrownBy(() -> service.rotate("old")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_unknownOrMissingToken_isInvalid() {
        assertThatThrownBy(() -> service.rotate(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate("unknown")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revoke_revokesFamilyAsLogout() {
        when(repository.findByTokenHash(RefreshTokenService.hash("t"))).thenReturn(Optional.of(token(now.plus(Duration.ofDays(1)))));

        service.revoke("t");

        verify(repository).revokeFamily(familyId, RevokeReason.LOGOUT, now);
    }

    private RefreshToken token(Instant expiresAt) {
        return new RefreshToken(userId, "hash", familyId, expiresAt);
    }
}
