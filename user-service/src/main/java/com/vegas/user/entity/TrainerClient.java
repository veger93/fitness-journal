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

import java.time.Instant;
import java.util.UUID;

/**
 * Связь "тренер — клиент" (таблица trainer_clients).
 * Активна, пока endedAt == null. Закончилась — строку не удаляем: это история, кто кого и когда тренировал.
 *
 * Тренер хранится просто id (его данные здесь не нужны), а клиент — связью @ManyToOne:
 * в списке "мои клиенты" нужны его имя и признак "ведомый".
 */
@Entity
@Table(name = "trainer_clients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainerClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trainer_id", nullable = false, updatable = false)
    private UUID trainerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private User client;

    /** Записная книжка тренера, по желанию */
    @Setter
    @Column(length = 30)
    private String phone;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    public TrainerClient(UUID trainerId, User client, String phone, Instant linkedAt) {
        this.trainerId = trainerId;
        this.client = client;
        this.phone = phone;
        this.linkedAt = linkedAt;
    }

    public UUID getClientId() {
        return client.getId();
    }
}
