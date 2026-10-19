package com.vegas.notification.service;

import com.vegas.notification.dto.NotificationResponse;
import com.vegas.notification.dto.PageResponse;
import com.vegas.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final NotificationRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> history(UUID userId, int page, int size) {
        return PageResponse.of(repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(NotificationResponse::from));
    }
}
