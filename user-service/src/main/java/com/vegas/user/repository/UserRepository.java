package com.vegas.user.repository;

import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
