--liquibase formatted sql

--changeset sergey:022-programs
-- Программа тренировок, подобранная по анкете (docs/program.md, вариант D).
-- Дни программы — обычные комплексы (workout_templates): старт тренировки, «Сегодня по плану», история
-- работают без переделок. Здесь — то, чего у комплекса нет: ответы анкеты, схема подходов, замены.
-- Ответы о здоровье (вопросы PAR-Q+) НЕ храним: ни одной колонки под них нет специально.
CREATE TABLE programs (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id         UUID        NOT NULL,
    status           VARCHAR(10) NOT NULL,          -- ACTIVE / ARCHIVED (прошёл анкету заново — старая в архив)
    goal             VARCHAR(20) NOT NULL,          -- MUSCLE / STRENGTH / FAT_LOSS
    days_per_week    SMALLINT    NOT NULL,
    session_minutes  SMALLINT    NOT NULL,
    location         VARCHAR(20) NOT NULL,          -- GYM / HOME_DUMBBELLS
    experience_level VARCHAR(20) NOT NULL,          -- копия на момент анкеты: генератор живёт здесь, в workout-service
    priority         VARCHAR(20),                   -- CHEST / BACK / LEGS / ARMS / SHOULDERS; NULL — «всё ровно»
    started_at       TIMESTAMPTZ NOT NULL,
    review_at        DATE        NOT NULL,          -- через 6 недель — «пора пересмотреть»
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_programs_status   CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_programs_goal     CHECK (goal IN ('MUSCLE', 'STRENGTH', 'FAT_LOSS')),
    CONSTRAINT ck_programs_days     CHECK (days_per_week BETWEEN 2 AND 5),
    CONSTRAINT ck_programs_minutes  CHECK (session_minutes IN (45, 60, 75)),
    CONSTRAINT ck_programs_location CHECK (location IN ('GYM', 'HOME_DUMBBELLS')),
    CONSTRAINT ck_programs_level    CHECK (experience_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_programs_priority CHECK (priority IN ('CHEST', 'BACK', 'LEGS', 'ARMS', 'SHOULDERS'))
);

-- Частичный уникальный индекс: АКТИВНАЯ программа у человека одна, архивных — сколько угодно.
-- Обычный UNIQUE (owner_id) запретил бы и архив; условие WHERE проверяет только строки со status = 'ACTIVE'.
CREATE UNIQUE INDEX uq_programs_active_owner ON programs (owner_id) WHERE status = 'ACTIVE';

-- «Что беспокоит» — набор значений: отдельная таблица (в JPA — @ElementCollection), а не строка через запятую:
-- так работают CHECK и поиск по значению.
CREATE TABLE program_limits (
    program_id UUID        NOT NULL,
    body_limit VARCHAR(20) NOT NULL,                -- KNEES / LOWER_BACK / SHOULDERS

    CONSTRAINT pk_program_limits PRIMARY KEY (program_id, body_limit),
    CONSTRAINT fk_program_limits_program FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE CASCADE,
    CONSTRAINT ck_program_limits_value CHECK (body_limit IN ('KNEES', 'LOWER_BACK', 'SHOULDERS'))
);

-- «Место» в программе: день (комплекс) + порядок. Упражнение на месте можно заменить ОДИН раз
-- и только на упражнение того же семейства движения (movement_pattern) — правило П14.
CREATE TABLE program_slots (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id           UUID        NOT NULL,
    template_id          UUID        NOT NULL,      -- комплекс дня («День A · всё тело»)
    position             INTEGER     NOT NULL,      -- порядок в дне, как в workout_template_exercises
    movement_pattern     VARCHAR(30) NOT NULL,      -- семейство: заменять можно только внутри него
    exercise_id          UUID        NOT NULL,
    original_exercise_id UUID,                      -- что стояло до замены; NULL — не меняли
    sets                 SMALLINT    NOT NULL,
    reps_min             SMALLINT    NOT NULL,
    reps_max             SMALLINT    NOT NULL,
    rest_sec             SMALLINT    NOT NULL,
    swapped              BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_program_slots_program  FOREIGN KEY (program_id) REFERENCES programs (id) ON DELETE CASCADE,
    -- человек удалил комплекс дня — его места в программе больше не нужны
    CONSTRAINT fk_program_slots_template FOREIGN KEY (template_id) REFERENCES workout_templates (id) ON DELETE CASCADE,
    CONSTRAINT fk_program_slots_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id),
    CONSTRAINT fk_program_slots_original FOREIGN KEY (original_exercise_id) REFERENCES exercises (id),
    CONSTRAINT uq_program_slots_position UNIQUE (template_id, position),
    CONSTRAINT ck_program_slots_sets     CHECK (sets BETWEEN 1 AND 10),
    CONSTRAINT ck_program_slots_reps     CHECK (reps_min >= 1 AND reps_min <= reps_max AND reps_max <= 50),
    CONSTRAINT ck_program_slots_rest     CHECK (rest_sec BETWEEN 30 AND 600),
    -- заменено <=> известно, что было до замены: одно без другого — ошибка в коде
    CONSTRAINT ck_program_slots_swap     CHECK (swapped = (original_exercise_id IS NOT NULL))
);

CREATE INDEX idx_program_slots_program ON program_slots (program_id);

-- «Больше не предлагать»: генератор и окно замены это упражнение не возьмут.
CREATE TABLE exercise_exclusions (
    owner_id    UUID        NOT NULL,
    exercise_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_exercise_exclusions PRIMARY KEY (owner_id, exercise_id),
    -- своё упражнение без истории удаляется насовсем (docs/catalog.md) — исключение уходит вместе с ним
    CONSTRAINT fk_exercise_exclusions_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE
);
--rollback DROP TABLE exercise_exclusions; DROP TABLE program_slots; DROP TABLE program_limits; DROP TABLE programs;
