package com.vegas.traineraccess;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrainerLinkRepository extends JpaRepository<TrainerLink, UUID> {

    /** Главная проверка доступа: этот клиент — клиент этого тренера? Ключ — clientId, поиск по первичному ключу */
    boolean existsByClientIdAndTrainerId(UUID clientId, UUID trainerId);
}
