package com.vegas.notification.service;

import com.vegas.notification.entity.EmailMessage;
import com.vegas.notification.entity.Notification;
import com.vegas.notification.entity.NotificationType;
import com.vegas.notification.entity.PlannedWorkout;
import com.vegas.notification.entity.WeeklyPlan;
import com.vegas.notification.repository.NotificationSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestReminderServiceTest {

    // вторник
    private static final Instant NOW = Instant.parse("2026-10-27T10:00:00Z");
    private static final UUID USER = UUID.randomUUID();

    @Mock
    private NotificationSettingsRepository settingsRepository;
    @Mock
    private WeeklyPlanSyncService planService;
    @Mock
    private NotificationService notificationService;
    private TestReminderService service;

    @BeforeEach
    void setUp() {
        service = new TestReminderService(settingsRepository, planService, notificationService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @SuppressWarnings("unchecked")
    void send_uniqueKey_andTemplateFromTodaysPlan() {
        WeeklyPlan plan = new WeeklyPlan(USER);
        plan.replace(List.of(new PlannedWorkout(DayOfWeek.TUESDAY, "Ноги")), NOW);
        when(planService.find(USER)).thenReturn(Optional.of(plan));
        Notification saved = org.mockito.Mockito.mock(Notification.class);
        when(notificationService.send(eq(USER), eq(NotificationType.WORKOUT_REMINDER), eq("TEST:" + NOW), any()))
                .thenReturn(Optional.of(saved));

        service.send(USER);

        ArgumentCaptor<Function<String, EmailMessage>> template = ArgumentCaptor.forClass(Function.class);
        verify(notificationService).send(eq(USER), eq(NotificationType.WORKOUT_REMINDER), eq("TEST:" + NOW), template.capture());
        // письмо то же, что придёт по расписанию: с комплексом из плана на сегодня
        assertThat(template.getValue().apply("Сергей").subject()).isEqualTo("Сегодня по плану: Ноги");
    }
}
