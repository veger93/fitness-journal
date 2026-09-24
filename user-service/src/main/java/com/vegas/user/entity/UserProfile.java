package com.vegas.user.entity;

import com.vegas.common.model.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
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
 * Физ. данные пользователя (таблица user_profiles), связь 1:1 с User.
 *
 * @MapsId: id профиля = id пользователя. Отдельный ключ не нужен,
 * а найти профиль можно сразу по userId: profileRepository.findById(userId).
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {

    @Id
    @Setter(AccessLevel.NONE)
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false) // LAZY: не грузим User, пока он явно не нужен
    @JoinColumn(name = "user_id")
    @Setter(AccessLevel.NONE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "birth_year")
    private Short birthYear;

    @Column(name = "height_cm")
    private Short heightCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", length = 20)
    private ExperienceLevel experienceLevel;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    /** Пустой профиль создаём сразу при регистрации, заполняется на онбординге. */
    public UserProfile(User user) {
        this.user = user;
    }
}
