--liquibase formatted sql

--changeset sergey:006-add-running-metrics
-- Бег и упражнения на время: итог тренировки целиком, а не только "лучший подход".
-- total_distance_m / total_duration_sec — сумма по рабочим подходам (интервалы складываются);
-- best_pace_sec_per_km — лучший темп на отрезке от 1 км (короткий спринт не должен давать "рекорд темпа").
ALTER TABLE exercise_performances ADD COLUMN total_distance_m     INTEGER;
ALTER TABLE exercise_performances ADD COLUMN total_duration_sec   INTEGER;
ALTER TABLE exercise_performances ADD COLUMN best_pace_sec_per_km NUMERIC(6, 1);

-- уже записанные пробежки: обычно это одна запись, поэтому "лучшее" = "всего"
UPDATE exercise_performances
SET total_distance_m     = best_distance_m,
    total_duration_sec   = best_duration_sec,
    best_pace_sec_per_km = CASE
        WHEN best_distance_m >= 1000 AND best_duration_sec IS NOT NULL
            THEN round(best_duration_sec * 1000.0 / best_distance_m, 1)
        END
WHERE tracking_type = 'DISTANCE';
--rollback ALTER TABLE exercise_performances DROP COLUMN best_pace_sec_per_km, DROP COLUMN total_duration_sec, DROP COLUMN total_distance_m;
