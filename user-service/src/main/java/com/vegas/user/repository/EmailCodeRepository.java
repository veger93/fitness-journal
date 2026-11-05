package com.vegas.user.repository;

import com.vegas.user.entity.EmailCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailCodeRepository extends JpaRepository<EmailCode, UUID> {

    /** Последний отправленный код: действует только он (новый код отменяет старые) */
    Optional<EmailCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, EmailCode.Purpose purpose);

    /**
     * Уборка: удалить записи, истёкшие раньше cutoff (ExpiredTokensCleanupJob). Одним DELETE, без загрузки в память.
     */
    @Modifying
    @Query("delete from EmailCode t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
