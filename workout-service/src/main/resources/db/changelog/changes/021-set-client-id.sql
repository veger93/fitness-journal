--liquibase formatted sql

--changeset sergey:021-set-client-id
-- Защита от дублей подхода (идемпотентность). Телефон сам придумывает id подхода (UUID) и шлёт его в запросе.
-- Связь в зале плохая: ответ может потеряться, и телефон отправит тот же подход ещё раз. С тем же client_id
-- сервис не создаёт второй подход, а возвращает уже сохранённый. UNIQUE — страховка на случай, когда два
-- одинаковых запроса пришли одновременно: второй упрётся в индекс (409), телефон повторит и получит сохранённый.
-- NULL — старые подходы и запросы без client_id: в Postgres NULL-ы друг другу не равны, ограничение их не трогает.
ALTER TABLE workout_sets ADD COLUMN client_id UUID;
ALTER TABLE workout_sets ADD CONSTRAINT uq_workout_sets_client UNIQUE (workout_exercise_id, client_id);
--rollback ALTER TABLE workout_sets DROP CONSTRAINT uq_workout_sets_client; ALTER TABLE workout_sets DROP COLUMN client_id;
