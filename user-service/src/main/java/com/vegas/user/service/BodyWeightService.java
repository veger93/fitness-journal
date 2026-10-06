package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.user.dto.RecordWeightRequest;
import com.vegas.user.dto.WeightEntryResponse;
import com.vegas.user.entity.BodyWeightEntry;
import com.vegas.user.event.OutboxService;
import com.vegas.user.mapper.WeightMapper;
import com.vegas.user.repository.BodyWeightEntryRepository;
import com.vegas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * История веса тела. Правило: одна запись на день (см. uq_body_weight_user_date в миграции 003).
 */
@Service
@RequiredArgsConstructor
public class BodyWeightService {

    private final BodyWeightEntryRepository weightRepository;
    private final UserRepository userRepository;
    private final WeightMapper weightMapper;
    private final Clock clock;
    private final OutboxService outboxService;

    @Transactional
    public WeightEntryResponse record(UUID userId, LocalDate measuredOn, RecordWeightRequest request) {
        if (measuredOn.isAfter(LocalDate.now(clock))) {
            throw new BadRequestException("Дата замера не может быть в будущем");
        }
        BodyWeightEntry entry = upsert(userId, request.weightKg(), measuredOn);
        // текущий вес мог измениться -> снимок профиля для аналитики
        outboxService.profileUpdated(userId);
        return weightMapper.toResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<WeightEntryResponse> history(UUID userId) {
        return weightMapper.toResponses(weightRepository.findAllByUserIdOrderByMeasuredOnDesc(userId));
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> currentWeight(UUID userId) {
        return weightRepository.findFirstByUserIdOrderByMeasuredOnDesc(userId)
                .map(BodyWeightEntry::getWeightKg);
    }

    /**
     * Upsert = update или insert: запись за этот день есть -> обновляем вес, нет -> создаём.
     *
     * Обрати внимание: у найденной записи просто вызываем setter, без save().
     * Сущность "managed" (загружена в текущей транзакции), и Hibernate при коммите
     * сам заметит изменение и выполнит UPDATE. Это называется dirty checking.
     */
    @Transactional
    public BodyWeightEntry upsert(UUID userId, BigDecimal weightKg, LocalDate measuredOn) {
        return weightRepository.findByUserIdAndMeasuredOn(userId, measuredOn)
                .map(existing -> {
                    existing.setWeightKg(weightKg);
                    return existing;
                })
                .orElseGet(() -> weightRepository.save(new BodyWeightEntry(
                        // getReferenceById не делает SELECT: возвращает "заглушку" (proxy) с id.
                        // Для внешнего ключа user_id этого достаточно.
                        userRepository.getReferenceById(userId),
                        weightKg,
                        measuredOn
                )));
    }
}
