--liquibase formatted sql

--changeset sergey:007-create-workouts
-- Тренировка (сессия). Живёт в двух состояниях: IN_PROGRESS -> COMPLETED.
CREATE TABLE workouts (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    template_id  UUID,                                    -- из какого комплекса начата (может быть NULL)
    name         VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    started_at   TIMESTAMPTZ  NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- комплекс удалили -> тренировки остаются в истории, просто теряют ссылку
    CONSTRAINT fk_workouts_template      FOREIGN KEY (template_id) REFERENCES workout_templates (id) ON DELETE SET NULL,
    CONSTRAINT ck_workouts_name          CHECK (btrim(name) <> ''),
    CONSTRAINT ck_workouts_status        CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    -- completed_at заполнен ТОГДА И ТОЛЬКО ТОГДА, когда тренировка завершена
    CONSTRAINT ck_workouts_completed_at  CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL)),
    CONSTRAINT ck_workouts_time_order    CHECK (completed_at IS NULL OR completed_at >= started_at)
);

-- ЧАСТИЧНЫЙ уникальный индекс: уникальность только среди строк WHERE status = 'IN_PROGRESS'.
-- Итог: у пользователя может быть сколько угодно завершённых тренировок, но незавершённая — максимум одна.
CREATE UNIQUE INDEX uq_workouts_one_in_progress ON workouts (user_id) WHERE status = 'IN_PROGRESS';
-- история тренировок пользователя, новые сверху
CREATE INDEX idx_workouts_user_started ON workouts (user_id, started_at DESC);
CREATE INDEX idx_workouts_template ON workouts (template_id);

-- Упражнение внутри тренировки (по порядку).
CREATE TABLE workout_exercises (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_id  UUID    NOT NULL,
    exercise_id UUID    NOT NULL,
    position    INTEGER NOT NULL,

    CONSTRAINT fk_workout_exercises_workout  FOREIGN KEY (workout_id)  REFERENCES workouts (id) ON DELETE CASCADE,
    CONSTRAINT fk_workout_exercises_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id),
    CONSTRAINT ck_workout_exercises_position CHECK (position >= 0),
    CONSTRAINT uq_workout_exercises_exercise UNIQUE (workout_id, exercise_id),
    -- DEFERRABLE: при удалении упражнения из середины позиции остальных сдвигаются,
    -- и на время транзакции две строки могут иметь одинаковую позицию
    CONSTRAINT uq_workout_exercises_position UNIQUE (workout_id, position) DEFERRABLE INITIALLY DEFERRED
);
-- для аналитики "вся история упражнения" и проверки FK при удалении упражнения
CREATE INDEX idx_workout_exercises_exercise ON workout_exercises (exercise_id);

-- Подход. Какие поля заполнены, зависит от tracking_type упражнения (вес×повторы / время / дистанция).
CREATE TABLE workout_sets (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_exercise_id UUID          NOT NULL,
    position            INTEGER       NOT NULL,           -- номер подхода: 0, 1, 2...
    weight_kg           NUMERIC(6, 2),                    -- NULL: упражнение со своим весом без отягощения
    reps                INTEGER,
    duration_sec        INTEGER,
    distance_m          INTEGER,
    rpe                 NUMERIC(3, 1),                    -- субъективная тяжесть 1..10 (необязательно)
    warmup              BOOLEAN       NOT NULL DEFAULT FALSE, -- разминочные не идут в аналитику
    completed_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_workout_sets_exercise   FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises (id) ON DELETE CASCADE,
    CONSTRAINT ck_workout_sets_position   CHECK (position >= 0),
    CONSTRAINT ck_workout_sets_weight     CHECK (weight_kg >= 0),
    CONSTRAINT ck_workout_sets_reps       CHECK (reps > 0),
    CONSTRAINT ck_workout_sets_duration   CHECK (duration_sec > 0),
    CONSTRAINT ck_workout_sets_distance   CHECK (distance_m > 0),
    CONSTRAINT ck_workout_sets_rpe        CHECK (rpe BETWEEN 1 AND 10),
    -- хоть что-то должно быть записано
    CONSTRAINT ck_workout_sets_has_metric CHECK (reps IS NOT NULL OR duration_sec IS NOT NULL OR distance_m IS NOT NULL),
    CONSTRAINT uq_workout_sets_position   UNIQUE (workout_exercise_id, position) DEFERRABLE INITIALLY DEFERRED
);
--rollback DROP TABLE workout_sets; DROP TABLE workout_exercises; DROP TABLE workouts;
