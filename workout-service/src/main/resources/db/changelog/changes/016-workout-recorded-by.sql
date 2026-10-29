--liquibase formatted sql

--changeset sergey:016-workout-recorded-by
-- Кто записал тренировку: NULL — сам пользователь, иначе id тренера, который вёл её за клиента (docs/trainer.md).
-- Для истории ("записал тренер") и будущего подтверждения тренировки клиентом.
ALTER TABLE workouts ADD COLUMN recorded_by UUID;
--rollback ALTER TABLE workouts DROP COLUMN recorded_by;
