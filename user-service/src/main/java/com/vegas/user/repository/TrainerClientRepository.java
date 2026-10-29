package com.vegas.user.repository;

import com.vegas.user.entity.TrainerClient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainerClientRepository extends JpaRepository<TrainerClient, UUID> {

    /**
     * Активные клиенты тренера, новые сверху.
     * @EntityGraph("client"): клиентов подгружаем тем же запросом (JOIN), иначе на каждого был бы
     * отдельный SELECT при обращении к имени — классическая проблема N+1.
     */
    @EntityGraph(attributePaths = "client")
    List<TrainerClient> findAllByTrainerIdAndEndedAtIsNullOrderByLinkedAtDesc(UUID trainerId);

    /**
     * Связь "этот тренер — этот клиент", если она активна. Нет — значит, клиент не его.
     * Client_Id: подчёркивание явно говорит Spring Data "поле id внутри связи client".
     */
    @EntityGraph(attributePaths = "client")
    Optional<TrainerClient> findByTrainerIdAndClient_IdAndEndedAtIsNull(UUID trainerId, UUID clientId);

    boolean existsByTrainerIdAndEndedAtIsNull(UUID trainerId);

    /** Активный тренер клиента (у клиента он один — см. частичный индекс в миграции 006) */
    Optional<TrainerClient> findByClient_IdAndEndedAtIsNull(UUID clientId);
}
