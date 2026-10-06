package com.vegas.analytics.entity;

import com.vegas.common.event.UserProfileUpdatedEvent;
import com.vegas.common.model.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Физ. данные пользователя глазами аналитики. id не генерируется — это userId из user-service.
 */
@Entity
@Table(name = "athlete_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AthleteProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "experience_level", length = 20)
    private String experienceLevel;

    @Column(name = "body_weight_kg", precision = 5, scale = 2)
    private BigDecimal bodyWeightKg;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static AthleteProfile from(UserProfileUpdatedEvent event) {
        AthleteProfile profile = new AthleteProfile();
        profile.userId = event.userId();
        profile.copyFrom(event);
        return profile;
    }

    /**
     * Применить событие, только если оно новее сохранённого.
     * Защита от "гонки": если событие доставили повторно или не по порядку, старые данные не затрут новые.
     *
     * @return true — данные обновлены
     */
    public boolean applyIfNewer(UserProfileUpdatedEvent event) {
        if (!event.updatedAt().isAfter(updatedAt)) {
            return false;
        }
        copyFrom(event);
        return true;
    }

    private void copyFrom(UserProfileUpdatedEvent event) {
        this.gender = event.gender();
        this.birthYear = event.birthYear();
        this.heightCm = event.heightCm();
        this.experienceLevel = event.experienceLevel();
        this.bodyWeightKg = event.bodyWeightKg();
        this.updatedAt = event.updatedAt();
    }
}
