--liquibase formatted sql

--changeset sergey:010-create-weekly-plan
-- План на неделю: какой комплекс в какой день недели. Нет строки на день — день отдыха.
-- Один комплекс на день (UNIQUE user_id + day_of_week). Комплекс удалили — день плана тоже исчезает (CASCADE).
CREATE TABLE weekly_plan_days (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL,
    day_of_week  VARCHAR(10) NOT NULL,
    template_id  UUID        NOT NULL,

    CONSTRAINT fk_weekly_plan_template FOREIGN KEY (template_id) REFERENCES workout_templates (id) ON DELETE CASCADE,
    CONSTRAINT uq_weekly_plan_user_day UNIQUE (user_id, day_of_week),
    CONSTRAINT ck_weekly_plan_day      CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);
CREATE INDEX idx_weekly_plan_template ON weekly_plan_days (template_id);
--rollback DROP TABLE weekly_plan_days;
