-- analytics_db: полная очистка статистики.
-- Заново она заполнится сама: workout-service отправит события из outbox в Kafka,
-- analytics-service их обработает (через 5–20 секунд после запуска сида).

\set ON_ERROR_STOP on
TRUNCATE exercise_performances;
TRUNCATE athlete_profiles;
\echo 'analytics_db: статистика очищена, ждём события из Kafka'
