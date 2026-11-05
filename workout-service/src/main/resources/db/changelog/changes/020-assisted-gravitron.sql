--liquibase formatted sql

--changeset sergey:020-assisted-flag
-- assisted: вес в подходе — ПОМОЩЬ (противовес гравитрона), а не нагрузка. Больше противовес — легче.
-- Аналитика считает рабочую нагрузку = вес тела − противовес (docs/analytics.md, «Гравитрон»).
ALTER TABLE exercises ADD COLUMN assisted BOOLEAN NOT NULL DEFAULT FALSE;
--rollback ALTER TABLE exercises DROP COLUMN assisted;

--changeset sergey:020-gravitron-exercises
-- Гравитрон из зала (список «Нет в каталоге»): подтягивания и брусья с противовесом.
-- Семейства — как у обычных подтягиваний и брусьев: сила поддерживает друг друга. Рисунков нет — заглушка.
INSERT INTO exercises (name, body_part, equipment, tracking_type, icon, movement_pattern, assisted)
VALUES
    ('Подтягивания в гравитроне', 'BACK', 'MACHINE', 'WEIGHT_REPS', 'machine', 'VERTICAL_PULL', TRUE),
    ('Отжимания на брусьях в гравитроне', 'CHEST', 'MACHINE', 'WEIGHT_REPS', 'machine', 'HORIZONTAL_PUSH', TRUE);

INSERT INTO exercise_muscles (exercise_id, muscle, role)
SELECT e.id, m.muscle, m.role
FROM (VALUES
    ('Подтягивания в гравитроне', 'LATS', 'PRIMARY'),
    ('Подтягивания в гравитроне', 'BICEPS', 'SECONDARY'),
    ('Подтягивания в гравитроне', 'UPPER_BACK', 'SECONDARY'),
    ('Отжимания на брусьях в гравитроне', 'CHEST', 'PRIMARY'),
    ('Отжимания на брусьях в гравитроне', 'TRICEPS', 'PRIMARY'),
    ('Отжимания на брусьях в гравитроне', 'FRONT_DELTS', 'SECONDARY')
) AS m(name, muscle, role)
JOIN exercises e ON e.name = m.name AND e.owner_id IS NULL;

INSERT INTO exercise_technique (exercise_id, setup, steps, mistakes)
SELECT e.id, t.setup, t.steps, t.mistakes
FROM (VALUES
    ('Подтягивания в гравитроне',
     'Выставьте противовес: чем он больше, тем легче. Встаньте коленями на платформу, возьмитесь за рукояти хватом чуть шире плеч.',
     ARRAY['На выдохе подтянитесь, пока подбородок не окажется над рукоятями; локти ведите вниз к рёбрам.', 'Внизу полностью выпрямите руки, но не расслабляйте плечи.', 'Становится легко — уменьшайте противовес.'],
     ARRAY['Рывки и раскачка за счёт платформы.', 'Неполная амплитуда: руки не выпрямляются внизу.']),
    ('Отжимания на брусьях в гравитроне',
     'Выставьте противовес: чем он больше, тем легче. Встаньте коленями на платформу, упритесь руками в рукояти, руки прямые.',
     ARRAY['На вдохе согните руки и опуститесь, пока плечо не станет параллельно полу.', 'Локти ведите назад, а не в стороны.', 'На выдохе выжмите себя вверх до прямых рук.'],
     ARRAY['Опускаться слишком глубоко — перегрузка плечевых суставов.', 'Сутулить плечи вперёд.'])
) AS t(name, setup, steps, mistakes)
JOIN exercises e ON e.name = t.name AND e.owner_id IS NULL;
--rollback DELETE FROM exercises WHERE owner_id IS NULL AND name IN ('Подтягивания в гравитроне', 'Отжимания на брусьях в гравитроне');
