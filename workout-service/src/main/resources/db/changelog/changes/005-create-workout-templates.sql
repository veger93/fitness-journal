--liquibase formatted sql

--changeset sergey:005-create-workout-templates
-- Комплекс (шаблон тренировки): набор упражнений в заданном порядке.
-- owner_id IS NULL -> готовый комплекс (системный), иначе "Мои комплексы" пользователя.
CREATE TABLE workout_templates (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id   UUID,
    name       VARCHAR(100) NOT NULL,
    color      VARCHAR(20)  NOT NULL DEFAULT 'violet',   -- ключ цвета плитки из палитры фронта
    icon       VARCHAR(30),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_workout_templates_name_not_blank CHECK (btrim(name) <> '')
);

CREATE UNIQUE INDEX uq_workout_templates_owner_name ON workout_templates (owner_id, lower(name)) NULLS NOT DISTINCT;

-- Связь многие-ко-многим "комплекс <-> упражнение" + порядок упражнений внутри комплекса.
CREATE TABLE workout_template_exercises (
    template_id UUID    NOT NULL,
    exercise_id UUID    NOT NULL,
    position    INTEGER NOT NULL,                          -- 0, 1, 2... порядок в комплексе

    CONSTRAINT pk_workout_template_exercises PRIMARY KEY (template_id, position),
    CONSTRAINT fk_wte_template FOREIGN KEY (template_id) REFERENCES workout_templates (id) ON DELETE CASCADE,
    -- без ON DELETE: упражнение, которое стоит в комплексе, удалить нельзя (RESTRICT по умолчанию)
    CONSTRAINT fk_wte_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id),
    CONSTRAINT ck_wte_position CHECK (position >= 0),
    -- Упражнение в комплексе — один раз.
    -- DEFERRABLE INITIALLY DEFERRED: проверка в момент COMMIT, а не после каждой строки.
    -- Иначе при перестановке A,B -> B,A промежуточное состояние (B,B) нарушило бы UNIQUE.
    CONSTRAINT uq_wte_template_exercise UNIQUE (template_id, exercise_id) DEFERRABLE INITIALLY DEFERRED
);

-- Индекс на внешний ключ: PostgreSQL сам его НЕ создаёт.
-- Нужен, чтобы быстро проверять "есть ли упражнение в каком-то комплексе" (при удалении упражнения).
CREATE INDEX idx_wte_exercise ON workout_template_exercises (exercise_id);
--rollback DROP TABLE workout_template_exercises; DROP TABLE workout_templates;
