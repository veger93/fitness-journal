package com.vegas.notification.service;

import com.vegas.common.event.ProgramChangedEvent;
import com.vegas.notification.entity.ProgramReview;
import com.vegas.notification.repository.ProgramReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Держит копию даты пересмотра программы по событиям ProgramChanged из workout-service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramReviewSyncService {

    private final ProgramReviewRepository repository;

    /**
     * Новая программа — сохраняем/заменяем дату. Отказ от программы — удаляем: напоминать не о чем.
     * Повтор того же события безопасен (идемпотентно), устаревшее — пропускаем по changedAt.
     */
    @Transactional
    public void apply(ProgramChangedEvent event) {
        var saved = repository.findById(event.userId());
        if (saved.isPresent() && event.changedAt().isBefore(saved.get().getUpdatedAt())) {
            log.info("Skipped outdated program event for user {} ({})", event.userId(), event.changedAt());
            return;
        }
        if (!event.hasProgram()) {
            saved.ifPresent(repository::delete);
            return;
        }
        saved.ifPresentOrElse(
                review -> review.replace(event.programId(), event.reviewAt(), event.changedAt()),
                () -> repository.save(new ProgramReview(event.userId(), event.programId(), event.reviewAt(),
                        event.changedAt())));
    }
}
