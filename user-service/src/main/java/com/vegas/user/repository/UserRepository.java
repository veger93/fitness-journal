package com.vegas.user.repository;

import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data сам пишет SQL по имени метода:
 * findByEmail -> SELECT ... FROM users WHERE email = ?
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    /** Сколько демо сейчас живо — общий лимит (docs/demo.md) */
    long countByAuthProviderAndDemoExpiresAtAfter(AuthProvider authProvider, Instant now);

    /** Истёкшие демо для удаления: сначала сами демо-пользователи (их клиенты удалятся вместе с ними) */
    List<User> findTop50ByDemoExpiresAtBeforeOrderByAuthProviderAsc(Instant cutoff);
}
