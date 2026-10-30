package com.vegas.traineraccess;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainerLinkRepository extends JpaRepository<TrainerLink, UUID> {

    /** Главная проверка доступа: этот клиент — клиент этого тренера? Ключ — clientId, поиск по первичному ключу */
    boolean existsByClientIdAndTrainerId(UUID clientId, UUID trainerId);

    /** Все клиенты тренера — для сводок по клиентам (сигналы в списке "Чей дневник открыть") */
    List<TrainerLink> findAllByTrainerId(UUID trainerId);
}
