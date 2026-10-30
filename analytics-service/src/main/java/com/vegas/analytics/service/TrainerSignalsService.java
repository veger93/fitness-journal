package com.vegas.analytics.service;

import com.vegas.analytics.dto.ClientSignalResponse;
import com.vegas.analytics.repository.ExercisePerformanceRepository;
import com.vegas.traineraccess.TrainerLink;
import com.vegas.traineraccess.TrainerLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Сигналы по всем клиентам тренера одним запросом (метки в списке "Чей дневник открыть").
 * Кто клиенты — из своей копии связей (trainer_links, обновляется из Kafka): в user-service не ходим.
 * Клиентов у тренера десятки, история каждого — сотни строк: считаем в памяти, это быстро.
 */
@Service
@RequiredArgsConstructor
public class TrainerSignalsService {

    private final TrainerLinkRepository trainerLinkRepository;
    private final ExercisePerformanceRepository performanceRepository;
    private final RecordsService recordsService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ClientSignalResponse> clientSignals(UUID trainerId) {
        Instant now = clock.instant();
        return trainerLinkRepository.findAllByTrainerId(trainerId).stream()
                .map(TrainerLink::getClientId)
                .map(clientId -> ClientSignalCalculator.signal(clientId,
                        performanceRepository.findByUserIdOrderByPerformedAtAsc(clientId),
                        recordsService.records(clientId, 1).stream().findFirst(),
                        now))
                .toList();
    }
}
