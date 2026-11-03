--liquibase formatted sql

--changeset sergey:018-catalog-plate-loaded-row
-- Тренажёр из зала (фото таблички «Row»): рычажная тяга сидя с упором для ног, вес — блинами.
-- Отдельное упражнение, а не "Тяга горизонтального блока": другая механика и веса, истории смешивать нельзя
-- (docs/catalog.md). Рисунка в стиле каталога нет — заглушка.
INSERT INTO exercises (name, body_part, equipment, tracking_type, icon)
VALUES ('Горизонтальная тяга в рычажном тренажёре', 'BACK', 'MACHINE', 'WEIGHT_REPS', 'machine');

INSERT INTO exercise_muscles (exercise_id, muscle, role)
SELECT e.id, m.muscle, m.role
FROM (VALUES
    ('LATS', 'PRIMARY'),
    ('UPPER_BACK', 'PRIMARY'),
    ('BICEPS', 'SECONDARY'),
    ('REAR_DELTS', 'SECONDARY')
) AS m(muscle, role)
JOIN exercises e ON e.name = 'Горизонтальная тяга в рычажном тренажёре' AND e.owner_id IS NULL;

INSERT INTO exercise_technique (exercise_id, setup, steps, mistakes)
SELECT e.id,
       'Отрегулируйте сиденье так, чтобы рукояти были на уровне низа груди. Сядьте, стопы упираются в платформу, '
           || 'спина прямая. Возьмитесь за рукояти, руки прямые.',
       ARRAY['На выдохе потяните рукояти к поясу, локти ведите назад вдоль корпуса.',
             'В конце сведите лопатки и задержитесь на секунду.',
             'На вдохе плавно верните рукояти вперёд до прямых рук.'],
       ARRAY['Откидываться назад и тянуть корпусом.', 'Поднимать плечи к ушам — включаются трапеции, а не спина.']
FROM exercises e
WHERE e.name = 'Горизонтальная тяга в рычажном тренажёре' AND e.owner_id IS NULL;
--rollback DELETE FROM exercises WHERE owner_id IS NULL AND name = 'Горизонтальная тяга в рычажном тренажёре';
