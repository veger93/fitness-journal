package com.vegas.notification.repository;

import com.vegas.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    boolean existsByUserIdAndDedupeKey(UUID userId, String dedupeKey);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Число на колокольчике. Spring Data сам строит "select count(*) ... where user_id = ? and read_at is null". */
    long countByUserIdAndReadAtIsNull(UUID userId);

    /**
     * Отметить все непрочитанные одним UPDATE, а не загружать каждое уведомление в память и менять по одному.
     * clearAutomatically: после массового UPDATE объекты в контексте Hibernate устарели — сбрасываем их.
     * Возвращает число обновлённых строк.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
