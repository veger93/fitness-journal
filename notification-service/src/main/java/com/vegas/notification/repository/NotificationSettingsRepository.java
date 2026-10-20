package com.vegas.notification.repository;

import com.vegas.notification.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, UUID> {

    List<NotificationSettings> findByRemindersEnabledTrue();

    List<NotificationSettings> findByInactivityEnabledTrue();
}
