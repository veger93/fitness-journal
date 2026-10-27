package com.vegas.notification.service;

import com.vegas.notification.dto.NotificationResponse;
import com.vegas.notification.dto.PageResponse;
import com.vegas.notification.dto.UnreadCountResponse;
import com.vegas.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/** Лента уведомлений в приложении: история, счётчик непрочитанных, "прочитать все". */
@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final NotificationRepository repository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> history(UUID userId, int page, int size) {
        return PageResponse.of(repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(UUID userId) {
        return new UnreadCountResponse(repository.countByUserIdAndReadAtIsNull(userId));
    }

    /**
     * Пользователь открыл ленту — всё, что в ней было, прочитано.
     * Повторный вызов ничего не меняет (обновляются только строки с read_at IS NULL) — запрос идемпотентный.
     */
    @Transactional
    public void markAllRead(UUID userId) {
        repository.markAllRead(userId, clock.instant());
    }
}
