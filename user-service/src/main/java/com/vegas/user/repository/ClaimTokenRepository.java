package com.vegas.user.repository;

import com.vegas.user.entity.ClaimToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ClaimTokenRepository extends JpaRepository<ClaimToken, UUID> {

    Optional<ClaimToken> findByTokenHash(String tokenHash);

    /** Новая ссылка отменяет старые неиспользованные — по старой QR-картинке уже не войти */
    @Modifying
    @Query("DELETE FROM ClaimToken t WHERE t.clientId = :clientId AND t.usedAt IS NULL")
    void deleteUnusedByClientId(UUID clientId);

    /**
     * Уборка: удалить записи, истёкшие раньше cutoff (ExpiredTokensCleanupJob). Одним DELETE, без загрузки в память.
     */
    @Modifying
    @Query("delete from ClaimToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
