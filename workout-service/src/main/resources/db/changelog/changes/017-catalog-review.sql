--liquibase formatted sql

--changeset sergey:017-catalog-review-fixes
-- Ревизия каталога (ноябрь 2026, docs/catalog.md): Сергей проверил все 97 упражнений, нашёл 5 ошибок.
-- Упражнения меняем по имени среди системных (owner_id IS NULL): id не меняется, поэтому история тренировок
-- и аналитика (они ссылаются на id) остаются как были.

-- 1. Тяга к подбородку (штанга и гантели): гриф/гантели поднимаем до верха груди, а не до низа
UPDATE exercise_technique t
SET steps = ARRAY['На выдохе потяните штангу вверх вдоль корпуса, локти ведите в стороны и вверх.',
                  'Поднимите гриф до верха груди — локти на уровне плеч и выше кистей.',
                  'На вдохе опустите.']
FROM exercises e
WHERE t.exercise_id = e.id AND e.owner_id IS NULL AND e.name = 'Тяга штанги к подбородку';

UPDATE exercise_technique t
SET steps = ARRAY['На выдохе потяните гантели вверх вдоль корпуса, локти ведите в стороны и вверх.',
                  'Поднимите гантели до верха груди — локти на уровне плеч и выше кистей.',
                  'На вдохе опустите.']
FROM exercises e
WHERE t.exercise_id = e.id AND e.owner_id IS NULL AND e.name = 'Тяга гантелей к подбородку';

-- 2. Бег: постановка стопы
UPDATE exercise_technique t
SET steps    = ARRAY['Держите корпус ровно, с лёгким наклоном вперёд.',
                     'Ставьте стопу под себя, на переднюю часть или середину стопы, не выбрасывая ногу далеко вперёд.',
                     'Руки согнуты примерно под 90° и двигаются вдоль корпуса.',
                     'Темп — такой, чтобы можно было говорить.'],
    mistakes = ARRAY['Приземляться на пятку далеко впереди корпуса — удар уходит в колени и поясницу.',
                     'Слишком быстрый старт.',
                     'Резкий рост километража — прибавляйте постепенно.']
FROM exercises e
WHERE t.exercise_id = e.id AND e.owner_id IS NULL AND e.name = 'Бег';

-- 3. Французский жим с гантелью: на старом рисунке был разгиб в наклоне. Новый рисунок (Everkinetic) — стоя,
--    поэтому упражнение называем без "сидя", а в технике — оба способа.
UPDATE exercises SET name = 'Французский жим гантели из-за головы'
WHERE owner_id IS NULL AND name = 'Французский жим гантели сидя';

UPDATE exercise_technique t
SET setup = 'Сядьте на скамью со спинкой (так проще держать корпус) или встаньте, стопы на ширине плеч. '
            || 'Возьмите одну гантель обеими руками за верхний диск и поднимите над головой.'
FROM exercises e
WHERE t.exercise_id = e.id AND e.owner_id IS NULL AND e.name = 'Французский жим гантели из-за головы';

UPDATE exercise_media m SET storage_key = 'overhead-dumbbell-extension'
FROM exercises e
WHERE m.exercise_id = e.id AND e.owner_id IS NULL AND e.name = 'Французский жим гантели из-за головы';

-- 4. Неверные рисунки убираем: жим стоя (был рисунок жима сидя), ножницы (был вариант лёжа на животе).
--    Нет строки — фронт показывает заглушку. Подходящих рисунков в том же стиле пока нет.
DELETE FROM exercise_media m
USING exercises e
WHERE m.exercise_id = e.id AND e.owner_id IS NULL AND e.name IN ('Жим штанги стоя', 'Ножницы');
--rollback UPDATE exercises SET name = 'Французский жим гантели сидя' WHERE owner_id IS NULL AND name = 'Французский жим гантели из-за головы';
--rollback UPDATE exercise_media m SET storage_key = 'seated-dumbbell-extension' FROM exercises e WHERE m.exercise_id = e.id AND e.owner_id IS NULL AND e.name = 'Французский жим гантели сидя';
--rollback INSERT INTO exercise_media (exercise_id, media_type, storage_key) SELECT e.id, 'IMAGE', k.key FROM (VALUES ('Жим штанги стоя', 'barbell-shoulder-press'), ('Ножницы', 'flutter-kicks')) AS k(name, key) JOIN exercises e ON e.name = k.name AND e.owner_id IS NULL;

--changeset sergey:017-catalog-new-machines
-- Тренажёры, которых не нашлось в зале (список «Нет в каталоге»). Рисунков пока нет — заглушка.
-- Рычажная вертикальная тяга — отдельное упражнение, а не "Тяга верхнего блока": другая механика и другие веса,
-- в одной истории они испортили бы прогресс и плато.
INSERT INTO exercises (name, body_part, equipment, tracking_type, icon)
VALUES
    ('Обратные разведения в тренажёре', 'SHOULDERS', 'MACHINE', 'WEIGHT_REPS', 'machine'),
    ('Вертикальная тяга в рычажном тренажёре', 'BACK', 'MACHINE', 'WEIGHT_REPS', 'machine');

INSERT INTO exercise_muscles (exercise_id, muscle, role)
SELECT e.id, m.muscle, m.role
FROM (VALUES
    ('Обратные разведения в тренажёре', 'REAR_DELTS', 'PRIMARY'),
    ('Обратные разведения в тренажёре', 'UPPER_BACK', 'SECONDARY'),
    ('Обратные разведения в тренажёре', 'TRAPS', 'SECONDARY'),
    ('Вертикальная тяга в рычажном тренажёре', 'LATS', 'PRIMARY'),
    ('Вертикальная тяга в рычажном тренажёре', 'BICEPS', 'SECONDARY'),
    ('Вертикальная тяга в рычажном тренажёре', 'UPPER_BACK', 'SECONDARY')
) AS m(name, muscle, role)
JOIN exercises e ON e.name = m.name AND e.owner_id IS NULL;

INSERT INTO exercise_technique (exercise_id, setup, steps, mistakes)
SELECT e.id, t.setup, t.steps, t.mistakes
FROM (VALUES
    ('Обратные разведения в тренажёре',
     'Тренажёр «бабочка»: рукояти переставьте назад. Сядьте лицом к спинке, грудь прижата. Рукояти на уровне плеч, руки почти прямые.',
     ARRAY['На выдохе разведите руки назад по дуге до линии корпуса.', 'В конце сведите лопатки и задержитесь на секунду.', 'На вдохе плавно верните руки.'],
     ARRAY['Отрывать грудь от спинки и помогать корпусом.', 'Слишком большой вес — работают трапеции, а не задняя дельта.']),
    ('Вертикальная тяга в рычажном тренажёре',
     'Сядьте, зафиксируйте бёдра под валиками. Возьмитесь за рукояти, руки прямые. Рычаги независимые — можно тянуть и одной рукой.',
     ARRAY['На выдохе потяните рукояти вниз к верху груди, локти ведите вниз и назад.', 'Внизу сведите лопатки.', 'На вдохе плавно верните рукояти вверх до прямых рук.'],
     ARRAY['Отклоняться назад и тянуть корпусом.', 'Бросать рукояти вверх — пропадает растяжение мышц.'])
) AS t(name, setup, steps, mistakes)
JOIN exercises e ON e.name = t.name AND e.owner_id IS NULL;
--rollback DELETE FROM exercises WHERE owner_id IS NULL AND name IN ('Обратные разведения в тренажёре', 'Вертикальная тяга в рычажном тренажёре');
