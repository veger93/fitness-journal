package com.vegas.user.repository;

import com.vegas.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Есть ли у семьи действующий токен: после выхода или тревоги — нет */
    boolean existsByFamilyIdAndRevokedAtIsNull(UUID familyId);

    /**
     * Отозвать все ещё действующие токены семьи одним UPDATE.
     * @Modifying — запрос меняет данные; clearAutomatically — после него сбросить кэш Hibernate,
     * иначе загруженные раньше объекты остались бы "неотозванными" в памяти.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update RefreshToken t
            set t.revokedAt = :now, t.revokedReason = :reason
            where t.familyId = :familyId and t.revokedAt is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId,
                     @Param("reason") RefreshToken.RevokeReason reason,
                     @Param("now") Instant now);
}
