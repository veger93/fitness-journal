--liquibase formatted sql

--changeset sergey:001-create-exercise-performances
-- Итог по одному упражнению в одной тренировке — "точка" на графике прогресса.
-- Заполняется из событий WorkoutCompleted (Kafka). Это КОПИЯ данных workout-service,
-- разложенная так, как удобно аналитике (read model).
CREATE TABLE exercise_performances (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID          NOT NULL,
    workout_id        UUID          NOT NULL,
    exercise_id       UUID          NOT NULL,
    exercise_name     VARCHAR(100)  NOT NULL,               -- копия названия: не ходим за ним в workout-service
    body_part         VARCHAR(20)   NOT NULL,
    tracking_type     VARCHAR(20)   NOT NULL,
    performed_at      TIMESTAMPTZ   NOT NULL,               -- когда завершена тренировка
    working_sets      INTEGER       NOT NULL,
    total_reps        INTEGER,
    volume_kg         NUMERIC(10,2) NOT NULL DEFAULT 0,      -- тоннаж: сумма вес × повторы
    best_weight_kg    NUMERIC(6,2),                         -- лучший подход (по расчётному 1ПМ)
    best_reps         INTEGER,
    best_e1rm_kg      NUMERIC(7,2),                         -- расчётный 1ПМ лучшего подхода
    best_duration_sec INTEGER,
    best_distance_m   INTEGER,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),

    -- Упражнение в тренировке учитывается один раз. Это же даёт индекс по workout_id для удаления.
    CONSTRAINT uq_exercise_performances_workout_exercise UNIQUE (workout_id, exercise_id),
    CONSTRAINT ck_exercise_performances_sets CHECK (working_sets > 0)
);

-- график прогресса: одно упражнение пользователя по времени
CREATE INDEX idx_exercise_performances_user_exercise ON exercise_performances (user_id, exercise_id, performed_at);
-- лента рекордов и сводки: всё по пользователю по времени
CREATE INDEX idx_exercise_performances_user_performed ON exercise_performances (user_id, performed_at);
--rollback DROP TABLE exercise_performances;
