--liquibase formatted sql

--changeset sergey:006-seed-system-templates
-- Готовые комплексы (чипсы "Готовые комплексы" и карточки на первом главном экране).
INSERT INTO workout_templates (name, color, icon)
VALUES
    ('Ноги', 'violet', 'steps'),
    ('Торс', 'green', 'dumbbell'),
    ('Спина', 'blue', 'pulldown'),
    ('Руки', 'orange', 'dumbbell');

INSERT INTO workout_template_exercises (template_id, exercise_id, position)
SELECT t.id, e.id, v.position
FROM (VALUES
    ('Ноги', 'Приседания со штангой', 0),
    ('Ноги', 'Жим ногами', 1),
    ('Ноги', 'Румынская тяга', 2),
    ('Торс', 'Жим лёжа', 0),
    ('Торс', 'Тяга штанги в наклоне', 1),
    ('Торс', 'Жим штанги стоя', 2),
    ('Торс', 'Подтягивания', 3),
    ('Спина', 'Становая тяга', 0),
    ('Спина', 'Подтягивания', 1),
    ('Спина', 'Тяга верхнего блока', 2),
    ('Руки', 'Подъём штанги на бицепс', 0),
    ('Руки', 'Французский жим', 1),
    ('Руки', 'Молотки с гантелями', 2)
) AS v(template_name, exercise_name, position)
JOIN workout_templates t ON t.name = v.template_name  AND t.owner_id IS NULL
JOIN exercises e         ON e.name = v.exercise_name AND e.owner_id IS NULL;
--rollback DELETE FROM workout_templates WHERE owner_id IS NULL;
