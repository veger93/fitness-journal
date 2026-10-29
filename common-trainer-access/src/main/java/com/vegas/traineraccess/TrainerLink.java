package com.vegas.traineraccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Копия связи "тренер — клиент" (таблица trainer_links). Источник правды — user-service */
@Entity
@Table(name = "trainer_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainerLink {

    @Id
    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "trainer_id", nullable = false)
    private UUID trainerId;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    public TrainerLink(UUID clientId, UUID trainerId, Instant linkedAt) {
        this.clientId = clientId;
        this.trainerId = trainerId;
        this.linkedAt = linkedAt;
    }
}
