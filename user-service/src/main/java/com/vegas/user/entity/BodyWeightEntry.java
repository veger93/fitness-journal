package com.vegas.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Одна запись веса тела (таблица body_weight_log). Много записей -> один User.
 * Вес — BigDecimal, а не double: 80.1 в double хранится как 80.09999..., для денег и замеров это плохо.
 */
@Entity
@Table(name = "body_weight_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyWeightEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Setter // вес за день можно поправить
    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public BodyWeightEntry(User user, BigDecimal weightKg, LocalDate measuredOn) {
        this.user = user;
        this.weightKg = weightKg;
        this.measuredOn = measuredOn;
    }
}
