package com.vegas.notification.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.notification.dto.NotificationSettingsRequest;
import com.vegas.notification.dto.NotificationSettingsResponse;
import com.vegas.notification.entity.NotificationSettings;
import com.vegas.notification.repository.NotificationSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSettingsServiceTest {

    private static final UUID USER = UUID.randomUUID();

    @Mock
    private NotificationSettingsRepository repository;
    @InjectMocks
    private NotificationSettingsService service;

    @Test
    void get_withoutRow_returnsDefaults() {
        when(repository.findById(USER)).thenReturn(Optional.empty());

        NotificationSettingsResponse settings = service.get(USER);

        assertThat(settings.remindersEnabled()).isFalse();
        assertThat(settings.inactivityDays()).isEqualTo(NotificationSettings.DEFAULT_INACTIVITY_DAYS);
        assertThat(settings.timeZone()).isEqualTo("UTC");
        verify(repository, never()).save(any());
    }

    @Test
    void update_existingRow() {
        NotificationSettings existing = NotificationSettings.defaults(USER);
        when(repository.findById(USER)).thenReturn(Optional.of(existing));

        NotificationSettingsResponse result = service.update(USER, new NotificationSettingsRequest(
                true, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), LocalTime.of(19, 0), true, 5, "Europe/Moscow"));

        assertThat(result.reminderDays()).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.THURSDAY);
        assertThat(existing.getTimeZone()).isEqualTo("Europe/Moscow");
        assertThat(existing.getInactivityDays()).isEqualTo(5);
    }

    @Test
    void remindersWithoutDays_areRejected() {
        assertThatThrownBy(() -> NotificationSettingsService.validate(new NotificationSettingsRequest(
                true, Set.of(), LocalTime.of(19, 0), false, 7, "UTC")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void remindersWithoutTime_areRejected() {
        assertThatThrownBy(() -> NotificationSettingsService.validate(new NotificationSettingsRequest(
                true, Set.of(DayOfWeek.MONDAY), null, false, 7, "UTC")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownTimeZone_isRejected() {
        assertThatThrownBy(() -> NotificationSettingsService.validate(new NotificationSettingsRequest(
                false, null, null, false, 7, "Mars/Olympus")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mars/Olympus");
    }
}
