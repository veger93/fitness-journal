-- user_db: оставляем ТОЛЬКО пользователя :user_id, заполняем профиль и историю веса за 16 недель.
-- Запуск: см. infra/seed/README.md

\set ON_ERROR_STOP on
BEGIN;

-- остальные пользователи удаляются вместе с профилями и весом (ON DELETE CASCADE)
DELETE FROM users WHERE id <> :'user_id'::uuid;

-- Профиль: что пользователь уже заполнил — сохраняем (COALESCE берёт первое не-NULL значение),
-- пустое заполняем типовыми значениями, онбординг считаем пройденным.
UPDATE user_profiles
SET gender               = COALESCE(gender, 'MALE'),
    birth_year           = COALESCE(birth_year, 1995),
    height_cm            = COALESCE(height_cm, 178),
    experience_level     = COALESCE(experience_level, 'INTERMEDIATE'),
    onboarding_completed = TRUE
WHERE user_id = :'user_id'::uuid;

-- История веса: раз в неделю, плавное снижение с 84 до ~81.5 кг с небольшими колебаниями.
-- generate_series(0, 15) — "виртуальная таблица" из чисел 0..15, по строке на неделю.
DELETE FROM body_weight_log WHERE user_id = :'user_id'::uuid;
INSERT INTO body_weight_log (user_id, weight_kg, measured_on)
SELECT :'user_id'::uuid,
       round((84.0 - week * 0.17 + CASE WHEN week % 3 = 0 THEN 0.3 ELSE -0.1 END)::numeric, 1),
       current_date - (15 - week) * 7
FROM generate_series(0, 15) AS week;

-- Событие для аналитики: снимок профиля (как его пишет OutboxService в user-service).
-- Без него analytics-service не узнает уровень подготовки и возьмёт "средний".
DELETE FROM outbox_events;
INSERT INTO outbox_events (aggregate_id, event_type, event_key, payload, created_at)
SELECT p.user_id,
       'UserProfileUpdated',
       p.user_id::text,
       jsonb_build_object(
           'userId',          p.user_id,
           'gender',          p.gender,
           'birthYear',       p.birth_year,
           'heightCm',        p.height_cm,
           'experienceLevel', p.experience_level,
           'bodyWeightKg',    (SELECT weight_kg FROM body_weight_log w
                               WHERE w.user_id = p.user_id ORDER BY measured_on DESC LIMIT 1),
           'updatedAt',       to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
       )::text,
       now()
FROM user_profiles p
WHERE p.user_id = :'user_id'::uuid;

COMMIT;

\echo 'user_db: профиль заполнен, 16 записей веса, событие профиля в outbox'
