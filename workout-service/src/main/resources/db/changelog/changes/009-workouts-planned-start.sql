--liquibase formatted sql

--changeset sergey:009-workouts-planned-start
-- Тренировку можно подготовить заранее (выбрать упражнения в пробке), а время пойдёт, когда начнёшь.
-- started_at = NULL — тренировка ещё не начата. Начинается кнопкой "Начать" или первым записанным подходом.
ALTER TABLE workouts ALTER COLUMN started_at DROP NOT NULL;
-- завершённая тренировка всегда начата (без подходов завершить нельзя, а первый подход её начинает)
ALTER TABLE workouts ADD CONSTRAINT ck_workouts_completed_started CHECK (status <> 'COMPLETED' OR started_at IS NOT NULL);
--rollback ALTER TABLE workouts DROP CONSTRAINT ck_workouts_completed_started;
--rollback ALTER TABLE workouts ALTER COLUMN started_at SET NOT NULL;
