package com.vegas.notification.service;

import com.vegas.common.event.MonthlyReportReadyEvent;
import com.vegas.common.event.OverloadRiskEvent;
import com.vegas.common.event.PlateauDetectedEvent;
import com.vegas.notification.entity.EmailMessage;
import com.vegas.notification.entity.NotificationType;
import com.vegas.traineraccess.TrainerLink;
import com.vegas.traineraccess.TrainerLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsNotifierTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID EXERCISE = UUID.randomUUID();

    @Mock
    private NotificationService notificationService;
    @Mock
    private TrainerLinkRepository trainerLinks;
    private AnalyticsNotifier notifier;

    @BeforeEach
    void setUp() {
        // создаём руками: адрес фронта — обычная строка, Mockito её не подставит
        notifier = new AnalyticsNotifier(notificationService, trainerLinks, "http://localhost:5173");
    }

    @Test
    void plateauKey_dependsOnStartOfPlateau() {
        notifier.plateau(new PlateauDetectedEvent(USER, EXERCISE, "Присед", Instant.parse("2026-09-01T09:00:00Z"), 24));

        verify(notificationService).send(eq(USER), eq(NotificationType.PLATEAU),
                eq("PLATEAU:" + EXERCISE + ":2026-09-01T09:00:00Z"), any());
    }

    @Test
    void riskKey_isWeekly() {
        // понедельник и воскресенье одной ISO-недели -> один ключ
        assertThat(AnalyticsNotifier.isoWeek(risk("2026-09-28T08:00:00Z")))
                .isEqualTo(AnalyticsNotifier.isoWeek(risk("2026-10-04T20:00:00Z")))
                .isEqualTo("2026-W40");
        assertThat(AnalyticsNotifier.isoWeek(risk("2026-10-05T08:00:00Z"))).isEqualTo("2026-W41");
    }

    @Test
    void reportKey_isMonth() {
        notifier.monthlyReport(new MonthlyReportReadyEvent(USER, "2026-09", null, null, null, 3, 0));

        verify(notificationService).send(eq(USER), eq(NotificationType.MONTHLY_REPORT), eq("REPORT:2026-09"), any());
    }

    @Test
    void clientWithTrainer_trainerGetsInAppNotification_withClientName() {
        UUID trainer = UUID.randomUUID();
        when(trainerLinks.findById(USER)).thenReturn(Optional.of(new TrainerLink(USER, trainer, Instant.now(), "Ольга")));

        notifier.plateau(new PlateauDetectedEvent(USER, EXERCISE, "Присед", Instant.parse("2026-09-01T09:00:00Z"), 24));

        // клиенту — как обычно
        verify(notificationService).send(eq(USER), eq(NotificationType.PLATEAU), any(), any());
        // тренеру — в ленту, с именем клиента и своим ключом
        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(notificationService).notifyTrainer(eq(trainer), eq(USER), eq("Ольга"), eq(NotificationType.PLATEAU),
                eq("CLIENT:" + USER + ":PLATEAU:" + EXERCISE + ":2026-09-01T09:00:00Z"), message.capture());
        assertThat(message.getValue().subject()).isEqualTo("Ольга: плато");
        assertThat(message.getValue().body()).isEqualTo("Присед — без нового максимума 24 дн.");
    }

    @Test
    void clientWithoutTrainer_onlyClientNotified() {
        when(trainerLinks.findById(USER)).thenReturn(Optional.empty());

        notifier.overloadRisk(risk("2026-10-05T08:00:00Z"));

        verify(notificationService).send(eq(USER), eq(NotificationType.OVERLOAD_RISK), any(), any());
        verify(notificationService, never()).notifyTrainer(any(), any(), any(), any(), any(), any());
    }

    @Test
    void oldLinkWithoutName_fallbackName() {
        UUID trainer = UUID.randomUUID();
        when(trainerLinks.findById(USER)).thenReturn(Optional.of(new TrainerLink(USER, trainer, Instant.now(), null)));

        notifier.overloadRisk(risk("2026-10-05T08:00:00Z"));

        verify(notificationService).notifyTrainer(eq(trainer), eq(USER), eq(AnalyticsNotifier.UNKNOWN_CLIENT), any(), any(), any());
    }

    private static OverloadRiskEvent risk(String at) {
        return new OverloadRiskEvent(USER, EXERCISE, "Присед", "HIGH", List.of("x"), Instant.parse(at));
    }
}
