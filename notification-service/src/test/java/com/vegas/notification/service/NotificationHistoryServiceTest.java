package com.vegas.notification.service;

import com.vegas.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T18:00:00Z");
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private NotificationRepository repository;
    private NotificationHistoryService service;

    @BeforeEach
    void setUp() {
        service = new NotificationHistoryService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void unreadCount_fromRepository() {
        when(repository.countByUserIdAndReadAtIsNull(USER)).thenReturn(3L);

        assertThat(service.unreadCount(USER).count()).isEqualTo(3);
    }

    @Test
    void markAllRead_usesCurrentTime() {
        service.markAllRead(USER);

        // время прочтения берём из Clock — в тесте оно фиксированное и проверяемое
        verify(repository).markAllRead(USER, NOW);
    }
}
