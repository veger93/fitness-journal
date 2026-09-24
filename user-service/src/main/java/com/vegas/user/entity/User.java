package com.vegas.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Учётная запись пользователя (таблица users).
 *
 * Почему НЕ @Data: он генерирует equals/hashCode/toString по всем полям —
 * для JPA-сущностей это ломает Set-ы и может вызвать ленивую загрузку связей.
 * Поэтому только @Getter и точечные @Setter.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA нужен пустой конструктор, но снаружи им пользоваться не даём
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING) // храним 'LOCAL', а не 0 — порядок enum можно менять без порчи данных
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    @Column(name = "provider_id")
    private String providerId;

    @Setter
    @Column(name = "display_name", length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Регистрация по email + паролю. */
    public static User local(String email, String passwordHash, String displayName) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.authProvider = AuthProvider.LOCAL;
        user.role = Role.USER;
        return user;
    }

    /** Вход через внешнего провайдера (Google). Пароля нет. */
    public static User oauth(AuthProvider provider, String providerId, String email, String displayName) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.authProvider = provider;
        user.providerId = providerId;
        user.displayName = displayName;
        user.role = Role.USER;
        return user;
    }

    /** "Ivan@Mail.ru " и "ivan@mail.ru" — один и тот же пользователь. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
