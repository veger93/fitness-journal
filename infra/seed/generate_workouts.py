#!/usr/bin/env python3
"""
Генератор тестовых тренировок для dev-окружения -> 02-workouts.sql.

Запуск (нужен только Python 3, без библиотек):
    python infra/seed/generate_workouts.py

Сценарий — 16 недель тренировок промежуточного атлета, чтобы на графиках
и в аналитике было что смотреть:
  * 4 готовых комплекса по кругу: Торс -> Ноги -> Спина -> Руки, тренировка раз в 2–3 дня;
  * на 9-й неделе — разгрузка: 70% веса, подходов вдвое меньше;
  * жим лёжа растёт ~10 недель, потом ПЛАТО (держится 5+ недель);
  * присед и становая растут всё время -> есть личные рекорды;
  * в последней тренировке ног — резкий рост объёма при просевшем весе -> риск перегрузки;
  * разминочные подходы и RPE заполнены.

Даты в SQL относительные (N дней назад от сегодня), поэтому данные всегда "свежие".
Генератор детерминированный: одинаковый запуск -> одинаковый SQL.
"""
import uuid
from pathlib import Path

NAMESPACE = uuid.UUID("6d7a0c1e-5b1f-4c7e-9a5e-000000000001")
WEEKS = 16
DELOAD_WEEK = 9          # 1-based
OUT = Path(__file__).with_name("02-workouts.sql")

TEMPLATES = {
    "Торс":  ["Жим лёжа", "Тяга штанги в наклоне", "Жим штанги стоя", "Подтягивания"],
    "Ноги":  ["Приседания со штангой", "Жим ногами", "Румынская тяга"],
    "Спина": ["Становая тяга", "Подтягивания", "Тяга верхнего блока"],
    "Руки":  ["Подъём штанги на бицепс", "Французский жим", "Молотки с гантелями"],
}
ROTATION = ["Торс", "Ноги", "Спина", "Руки"]

# название -> (стартовый вес, прибавка за неделю, шаг округления, схема повторов, разминка?)
# None вместо веса — упражнение со своим весом (стартовые повторы, прибавка повторов за 3 недели)
PROGRAM = {
    "Жим лёжа":                (70.0, 1.25, 2.5, [8, 8, 7, 6], True),
    "Тяга штанги в наклоне":   (60.0, 1.25, 2.5, [10, 10, 9], True),
    "Жим штанги стоя":         (40.0, 0.6, 2.5, [8, 8, 7], True),
    "Приседания со штангой":   (90.0, 2.5, 2.5, [5, 5, 5, 5], True),
    "Жим ногами":              (140.0, 5.0, 5.0, [10, 10, 10], False),
    "Румынская тяга":          (80.0, 1.9, 2.5, [8, 8, 8], True),
    "Становая тяга":           (110.0, 2.5, 2.5, [5, 5, 5], True),
    "Тяга верхнего блока":     (55.0, 1.0, 2.5, [10, 10, 10], False),
    "Подъём штанги на бицепс": (30.0, 0.6, 2.5, [10, 10, 9], False),
    "Французский жим":         (25.0, 0.6, 2.5, [10, 10, 9], False),
    "Молотки с гантелями":     (12.0, 0.3, 1.0, [12, 12, 10], False),
}
PULL_UPS = "Подтягивания"
BENCH_PLATEAU_FROM_WEEK = 11   # с этой недели жим лёжа перестаёт расти


def uid(*parts) -> str:
    return str(uuid.uuid5(NAMESPACE, "/".join(map(str, parts))))


def round_to(value: float, step: float) -> float:
    return round(round(value / step) * step, 2)


def training_days():
    """Дни (сколько дней назад), когда были тренировки: шаг 2, 2, 3 -> ~3 тренировки в неделю."""
    days, day, steps, i = [], WEEKS * 7, [2, 2, 3], 0
    while day >= 1:
        days.append(day)
        day -= steps[i % 3]
        i += 1
    return days


def week_of(days_ago: int) -> int:
    """1 = самая первая неделя, WEEKS = текущая."""
    return WEEKS - (days_ago - 1) // 7


def sets_for(name: str, week: int, deload: bool, is_last_legs: bool):
    """Список подходов: (вес, повторы, rpe, разминка)."""
    if name == PULL_UPS:
        reps = 6 + (week - 1) // 3                      # +1 повтор каждые 3 недели
        scheme = [reps, reps, max(reps - 2, 1)]
        if deload:
            scheme = scheme[:2]
        return [(None, r, 8.0 if j < len(scheme) - 1 else 9.0, False) for j, r in enumerate(scheme)]

    start, per_week, step, scheme, warmup = PROGRAM[name]
    effective_week = week - (1 if week > DELOAD_WEEK else 0)   # неделя разгрузки прогресс не двигает
    if name == "Жим лёжа" and week >= BENCH_PLATEAU_FROM_WEEK:
        effective_week = BENCH_PLATEAU_FROM_WEEK - 1           # плато: вес застрял
        scheme = [7, 7, 6, 6] if week % 2 else [8, 7, 6, 5]     # и повторов не больше, чем на пике
    weight = round_to(start + per_week * (effective_week - 1), step)

    sets = []
    if deload:
        weight = round_to(weight * 0.7, step)
        scheme = scheme[: max(1, (len(scheme) + 1) // 2)]
    if is_last_legs and name == "Приседания со штангой":
        weight = round_to(weight * 0.9, step)                  # тяжело идёт: вес на 10% ниже...
        scheme = [5, 5, 5, 5, 5, 5, 4]                         # ...а объём резко вырос
    if warmup:
        sets.append((round_to(weight * 0.5, step), 10, None, True))
    for j, reps in enumerate(scheme):
        last = j == len(scheme) - 1
        rpe = 6.0 if deload else (9.5 if is_last_legs else (8.5 if last else 7.5))
        sets.append((weight, reps, rpe, False))
    return sets


def sql_num(value):
    return "NULL" if value is None else f"{value:g}"


def main():
    days = training_days()
    last_legs_day = min(d for i, d in enumerate(days) if ROTATION[i % 4] == "Ноги")

    workouts, exercises, sets = [], [], []
    for i, days_ago in enumerate(days):
        template = ROTATION[i % 4]
        week = week_of(days_ago)
        deload = week == DELOAD_WEEK
        workout_id = uid("workout", days_ago)
        workouts.append(f"    ('{workout_id}', '{template}', {days_ago}, {70 + (i % 4) * 5})")

        for pos, name in enumerate(TEMPLATES[template]):
            we_id = uid("we", days_ago, name)
            exercises.append(f"    ('{we_id}', '{workout_id}', '{name}', {pos})")
            minute = pos * 15
            for set_pos, (weight, reps, rpe, warm) in enumerate(
                    sets_for(name, week, deload, days_ago == last_legs_day)):
                sets.append(f"    ('{we_id}', {set_pos}, {sql_num(weight)}, {reps}, "
                            f"{sql_num(rpe)}, {str(warm).lower()}, {minute + set_pos * 3})")

    # (в старых версиях Python внутри f-строки нельзя писать "\n".join — собираем заранее)
    workouts_sql, exercises_sql, sets_sql = ",\n".join(workouts), ",\n".join(exercises), ",\n".join(sets)
    sql = f"""-- СГЕНЕРИРОВАНО infra/seed/generate_workouts.py — не править руками, менять генератор.
-- Тренировки за {WEEKS} недель для пользователя :user_id + события в outbox (аналитика пересчитается через Kafka).
-- Запуск: см. infra/seed/README.md

\\set ON_ERROR_STOP on
SET client_min_messages = warning;  -- без NOTICE-сообщений от TRUNCATE ... CASCADE
BEGIN;

-- ---------- очистка ----------
-- TRUNCATE ... CASCADE: очищает таблицу и всё, что на неё ссылается (упражнения тренировок, подходы)
TRUNCATE workouts CASCADE;
TRUNCATE outbox_events;
-- свои комплексы и упражнения всех пользователей; системные (owner_id IS NULL) остаются
DELETE FROM workout_templates WHERE owner_id IS NOT NULL;
DELETE FROM exercises WHERE owner_id IS NOT NULL;

-- ---------- тренировки ----------
INSERT INTO workouts (id, user_id, template_id, name, status, started_at, completed_at)
SELECT v.id::uuid,
       :'user_id'::uuid,
       t.id,
       v.name,
       'COMPLETED',
       date_trunc('day', now()) - make_interval(days => v.days_ago) + interval '18 hours',
       date_trunc('day', now()) - make_interval(days => v.days_ago) + interval '18 hours'
           + make_interval(mins => v.duration_min)
FROM (VALUES
{workouts_sql}
) AS v(id, name, days_ago, duration_min)
LEFT JOIN workout_templates t ON t.name = v.name AND t.owner_id IS NULL;

INSERT INTO workout_exercises (id, workout_id, exercise_id, position)
SELECT v.id::uuid, v.workout_id::uuid, e.id, v.position
FROM (VALUES
{exercises_sql}
) AS v(id, workout_id, exercise_name, position)
JOIN exercises e ON e.name = v.exercise_name AND e.owner_id IS NULL;

INSERT INTO workout_sets (workout_exercise_id, position, weight_kg, reps, rpe, warmup, completed_at)
SELECT v.we_id::uuid, v.position, v.weight_kg, v.reps, v.rpe, v.warmup,
       w.started_at + make_interval(mins => v.minute)
FROM (VALUES
{sets_sql}
) AS v(we_id, position, weight_kg, reps, rpe, warmup, minute)
JOIN workout_exercises we ON we.id = v.we_id::uuid
JOIN workouts w           ON w.id = we.workout_id;

-- ---------- события для аналитики ----------
-- JSON собираем прямо в SQL в формате WorkoutCompletedEvent из common-lib.
-- OutboxRelay отправит их в Kafka в порядке created_at, analytics-service пересчитает статистику.
INSERT INTO outbox_events (aggregate_id, event_type, event_key, payload, created_at)
SELECT w.id,
       'WorkoutCompleted',
       w.user_id::text,
       jsonb_build_object(
           'workoutId',   w.id,
           'userId',      w.user_id,
           'completedAt', to_char(w.completed_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
           'exercises', (
               SELECT jsonb_agg(jsonb_build_object(
                          'exerciseId',   e.id,
                          'exerciseName', e.name,
                          'bodyPart',     e.body_part,
                          'trackingType', e.tracking_type,
                          'sets', (
                              SELECT jsonb_agg(jsonb_build_object(
                                         'weightKg',    s.weight_kg,
                                         'reps',        s.reps,
                                         'durationSec', s.duration_sec,
                                         'distanceM',   s.distance_m,
                                         'rpe',         s.rpe,
                                         'warmup',      s.warmup) ORDER BY s.position)
                              FROM workout_sets s
                              WHERE s.workout_exercise_id = we.id)
                      ) ORDER BY we.position)
               FROM workout_exercises we
               JOIN exercises e ON e.id = we.exercise_id
               WHERE we.workout_id = w.id)
       )::text,
       w.completed_at
FROM workouts w
WHERE w.user_id = :'user_id'::uuid;

COMMIT;

\\echo 'workout_db: тренировок {len(workouts)}, упражнений {len(exercises)}, подходов {len(sets)}'
"""
    OUT.write_text(sql, encoding="utf-8", newline="\n")
    print(f"{OUT.name}: {len(workouts)} workouts, {len(exercises)} exercises, {len(sets)} sets")


if __name__ == "__main__":
    main()
