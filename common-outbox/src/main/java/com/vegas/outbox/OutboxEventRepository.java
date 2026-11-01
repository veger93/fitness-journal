package com.vegas.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Нативный SQL: FOR UPDATE SKIP LOCKED в JPQL не выразить.
     * FOR UPDATE — блокируем выбранные строки до конца транзакции;
     * SKIP LOCKED — строки, уже заблокированные ДРУГИМ экземпляром сервиса, пропускаем.
     * Когда запустим 2+ экземпляра сервиса, они не отправят одно событие дважды.
     */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("limit") int limit);

    /** Уборка: отправленные события старше срока хранения (OutboxCleanup). Неотправленные не трогаем никогда */
    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
