--liquibase formatted sql

--changeset sergey:012-more-system-exercises
-- Ещё 22 системных упражнения: для них есть готовые иллюстрации (см. changeset ниже).
INSERT INTO exercises (name, body_part, equipment, tracking_type, icon)
VALUES
    ('Фронтальный присед', 'LEGS', 'BARBELL', 'WEIGHT_REPS', 'barbell'),
    ('Разгибание ног в тренажёре', 'LEGS', 'MACHINE', 'WEIGHT_REPS', 'machine'),
    ('Сгибание ног сидя', 'LEGS', 'MACHINE', 'WEIGHT_REPS', 'machine'),
    ('Подъём на носки сидя', 'LEGS', 'MACHINE', 'WEIGHT_REPS', 'machine'),
    ('Ягодичный мост', 'LEGS', 'BODYWEIGHT', 'WEIGHT_REPS', 'bodyweight'),
    ('Тяга горизонтального блока', 'BACK', 'MACHINE', 'WEIGHT_REPS', 'machine'),
    ('Подтягивания обратным хватом', 'BACK', 'BODYWEIGHT', 'WEIGHT_REPS', 'bodyweight'),
    ('Шраги со штангой', 'BACK', 'BARBELL', 'WEIGHT_REPS', 'barbell'),
    ('Тяга штанги к подбородку', 'SHOULDERS', 'BARBELL', 'WEIGHT_REPS', 'barbell'),
    ('Жим Арнольда', 'SHOULDERS', 'DUMBBELL', 'WEIGHT_REPS', 'dumbbell'),
    ('Подъём гантелей перед собой', 'SHOULDERS', 'DUMBBELL', 'WEIGHT_REPS', 'dumbbell'),
    ('Сведение рук в тренажёре', 'CHEST', 'MACHINE', 'WEIGHT_REPS', 'machine'),
    ('Жим лёжа узким хватом', 'ARMS', 'BARBELL', 'WEIGHT_REPS', 'barbell'),
    ('Сгибания на скамье Скотта', 'ARMS', 'BARBELL', 'WEIGHT_REPS', 'barbell'),
    ('Концентрированный подъём на бицепс', 'ARMS', 'DUMBBELL', 'WEIGHT_REPS', 'dumbbell'),
    ('Обратные отжимания от скамьи', 'ARMS', 'BODYWEIGHT', 'WEIGHT_REPS', 'bodyweight'),
    ('Боковая планка', 'CORE', 'BODYWEIGHT', 'TIME', 'bodyweight'),
    ('Тяга Т-грифа', 'BACK', 'BARBELL', 'WEIGHT_REPS', 'barbell'),
    ('Пуловер со штангой лёжа', 'CHEST', 'BARBELL', 'WEIGHT_REPS', 'barbell'),
    ('Разгибание руки с гантелью в наклоне', 'ARMS', 'DUMBBELL', 'WEIGHT_REPS', 'dumbbell'),
    ('Подъём ног лёжа', 'CORE', 'BODYWEIGHT', 'WEIGHT_REPS', 'bodyweight'),
    ('Супермен', 'BACK', 'BODYWEIGHT', 'WEIGHT_REPS', 'bodyweight');

INSERT INTO exercise_muscles (exercise_id, muscle, role)
SELECT e.id, m.muscle, m.role
FROM (VALUES
    ('Фронтальный присед', 'QUADS', 'PRIMARY'),
    ('Фронтальный присед', 'GLUTES', 'SECONDARY'),
    ('Фронтальный присед', 'ABS', 'SECONDARY'),
    ('Разгибание ног в тренажёре', 'QUADS', 'PRIMARY'),
    ('Сгибание ног сидя', 'HAMSTRINGS', 'PRIMARY'),
    ('Подъём на носки сидя', 'CALVES', 'PRIMARY'),
    ('Ягодичный мост', 'GLUTES', 'PRIMARY'),
    ('Ягодичный мост', 'HAMSTRINGS', 'SECONDARY'),
    ('Тяга горизонтального блока', 'UPPER_BACK', 'PRIMARY'),
    ('Тяга горизонтального блока', 'LATS', 'PRIMARY'),
    ('Тяга горизонтального блока', 'BICEPS', 'SECONDARY'),
    ('Подтягивания обратным хватом', 'LATS', 'PRIMARY'),
    ('Подтягивания обратным хватом', 'BICEPS', 'PRIMARY'),
    ('Шраги со штангой', 'TRAPS', 'PRIMARY'),
    ('Шраги со штангой', 'FOREARMS', 'SECONDARY'),
    ('Тяга штанги к подбородку', 'SIDE_DELTS', 'PRIMARY'),
    ('Тяга штанги к подбородку', 'TRAPS', 'SECONDARY'),
    ('Жим Арнольда', 'FRONT_DELTS', 'PRIMARY'),
    ('Жим Арнольда', 'SIDE_DELTS', 'SECONDARY'),
    ('Жим Арнольда', 'TRICEPS', 'SECONDARY'),
    ('Подъём гантелей перед собой', 'FRONT_DELTS', 'PRIMARY'),
    ('Сведение рук в тренажёре', 'CHEST', 'PRIMARY'),
    ('Жим лёжа узким хватом', 'TRICEPS', 'PRIMARY'),
    ('Жим лёжа узким хватом', 'CHEST', 'SECONDARY'),
    ('Жим лёжа узким хватом', 'FRONT_DELTS', 'SECONDARY'),
    ('Сгибания на скамье Скотта', 'BICEPS', 'PRIMARY'),
    ('Концентрированный подъём на бицепс', 'BICEPS', 'PRIMARY'),
    ('Обратные отжимания от скамьи', 'TRICEPS', 'PRIMARY'),
    ('Обратные отжимания от скамьи', 'FRONT_DELTS', 'SECONDARY'),
    ('Боковая планка', 'OBLIQUES', 'PRIMARY'),
    ('Боковая планка', 'ABS', 'SECONDARY'),
    ('Тяга Т-грифа', 'UPPER_BACK', 'PRIMARY'),
    ('Тяга Т-грифа', 'LATS', 'PRIMARY'),
    ('Тяга Т-грифа', 'BICEPS', 'SECONDARY'),
    ('Пуловер со штангой лёжа', 'CHEST', 'PRIMARY'),
    ('Пуловер со штангой лёжа', 'LATS', 'SECONDARY'),
    ('Пуловер со штангой лёжа', 'TRICEPS', 'SECONDARY'),
    ('Разгибание руки с гантелью в наклоне', 'TRICEPS', 'PRIMARY'),
    ('Подъём ног лёжа', 'ABS', 'PRIMARY'),
    ('Подъём ног лёжа', 'OBLIQUES', 'SECONDARY'),
    ('Супермен', 'LOWER_BACK', 'PRIMARY'),
    ('Супермен', 'GLUTES', 'SECONDARY')
) AS m(name, muscle, role)
JOIN exercises e ON e.name = m.name AND e.owner_id IS NULL;
--rollback DELETE FROM exercises WHERE owner_id IS NULL AND name IN ('Фронтальный присед', 'Разгибание ног в тренажёре', 'Сгибание ног сидя', 'Подъём на носки сидя', 'Ягодичный мост', 'Тяга горизонтального блока', 'Подтягивания обратным хватом', 'Шраги со штангой', 'Тяга штанги к подбородку', 'Жим Арнольда', 'Подъём гантелей перед собой', 'Сведение рук в тренажёре', 'Жим лёжа узким хватом', 'Сгибания на скамье Скотта', 'Концентрированный подъём на бицепс', 'Обратные отжимания от скамьи', 'Боковая планка', 'Тяга Т-грифа', 'Пуловер со штангой лёжа', 'Разгибание руки с гантелью в наклоне', 'Подъём ног лёжа', 'Супермен');

--changeset sergey:012-exercise-images
-- Картинки (два положения движения) для системных упражнений: иллюстрации Everkinetic, CC BY-SA.
-- Откуда взяты и что изменено — frontend/public/exercises/LICENSE.md.
-- storage_key — имя файла без номера фазы: фронт берёт /exercises/<key>-1.svg и <key>-2.svg.
-- Нет картинки (бег, планка, подъём ног в висе) — нет строки, фронт покажет заглушку.
INSERT INTO exercise_media (exercise_id, media_type, storage_key)
SELECT e.id, 'IMAGE', k.storage_key
FROM (VALUES
    ('Жим лёжа', 'bench-press'),
    ('Жим лёжа на наклонной скамье', 'incline-bench-press'),
    ('Жим гантелей лёжа', 'dumbbell-bench-press'),
    ('Разводка гантелей', 'dumbbell-flys'),
    ('Отжимания на брусьях', 'dips'),
    ('Отжимания от пола', 'push-ups'),
    ('Становая тяга', 'deadlift'),
    ('Подтягивания', 'pull-ups'),
    ('Тяга штанги в наклоне', 'barbell-row'),
    ('Тяга верхнего блока', 'lat-pulldown'),
    ('Гиперэкстензия', 'hyperextension'),
    ('Приседания со штангой', 'squat'),
    ('Жим ногами', 'leg-press'),
    ('Румынская тяга', 'romanian-deadlift'),
    ('Выпады с гантелями', 'lunges'),
    ('Сгибание ног в тренажёре', 'leg-curl'),
    ('Подъём на носки', 'calf-raise'),
    ('Жим штанги стоя', 'barbell-shoulder-press'),
    ('Жим гантелей сидя', 'dumbbell-shoulder-press'),
    ('Махи гантелями в стороны', 'lateral-raises'),
    ('Махи в наклоне', 'rear-delt-raises'),
    ('Подъём штанги на бицепс', 'barbell-curl'),
    ('Молотки с гантелями', 'hammer-curl'),
    ('Французский жим', 'french-press'),
    ('Разгибание рук на блоке', 'triceps-pushdown'),
    ('Скручивания', 'crunches'),
    ('Фронтальный присед', 'front-squat'),
    ('Разгибание ног в тренажёре', 'leg-extension'),
    ('Сгибание ног сидя', 'seated-leg-curl'),
    ('Подъём на носки сидя', 'seated-calf-raise'),
    ('Ягодичный мост', 'glute-bridge'),
    ('Тяга горизонтального блока', 'seated-cable-row'),
    ('Подтягивания обратным хватом', 'chin-ups'),
    ('Шраги со штангой', 'shrugs'),
    ('Тяга штанги к подбородку', 'upright-row'),
    ('Жим Арнольда', 'arnold-press'),
    ('Подъём гантелей перед собой', 'front-raises'),
    ('Сведение рук в тренажёре', 'butterfly'),
    ('Жим лёжа узким хватом', 'close-grip-bench-press'),
    ('Сгибания на скамье Скотта', 'preacher-curl'),
    ('Концентрированный подъём на бицепс', 'concentration-curl'),
    ('Обратные отжимания от скамьи', 'bench-dips'),
    ('Боковая планка', 'side-plank'),
    ('Тяга гантели одной рукой', 'dumbbell-row'),
    ('Тяга Т-грифа', 't-bar-row'),
    ('Пуловер со штангой лёжа', 'pullover'),
    ('Разгибание руки с гантелью в наклоне', 'triceps-kickback'),
    ('Подъём ног лёжа', 'lying-leg-raise'),
    ('Супермен', 'superman')
) AS k(name, storage_key)
JOIN exercises e ON e.name = k.name AND e.owner_id IS NULL;
--rollback DELETE FROM exercise_media WHERE media_type = 'IMAGE';
