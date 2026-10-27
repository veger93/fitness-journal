--liquibase formatted sql

--changeset sergey:011-create-deload-periods
-- Режим разгрузки по упражнению: с какого по какое время подсказывать облегчённые вес и схему.
-- План на неделю не меняется — разгрузка касается одного упражнения, где бы оно ни встретилось.
-- Активная = ends_at в будущем. Закончил раньше — ends_at ставим "сейчас", строку не удаляем (история разгрузок).
CREATE TABLE deload_periods (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID          NOT NULL,
    exercise_id        UUID          NOT NULL,
    working_weight_kg  NUMERIC(6, 2),             -- NULL: упражнение без веса (только повторы)
    sets               INTEGER       NOT NULL,
    reps               INTEGER       NOT NULL,
    started_at         TIMESTAMPTZ   NOT NULL,
    ends_at            TIMESTAMPTZ   NOT NULL,

    CONSTRAINT fk_deload_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT ck_deload_period   CHECK (ends_at >= started_at),
    CONSTRAINT ck_deload_scheme   CHECK (sets BETWEEN 1 AND 10 AND reps BETWEEN 1 AND 50)
);
-- "активные разгрузки пользователя": ищем по user_id и отсекаем закончившиеся по ends_at
CREATE INDEX idx_deload_user_ends ON deload_periods (user_id, ends_at);
--rollback DROP TABLE deload_periods;
