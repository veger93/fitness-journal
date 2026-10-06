--liquibase formatted sql

--changeset sergey:003-add-avg-rpe
-- Средний RPE рабочих подходов (если пользователь его указывал).
-- Высокий RPE несколько тренировок подряд — один из признаков накопленной усталости.
-- Для старых строк значения нет: RPE в аналитику раньше не передавался.
ALTER TABLE exercise_performances ADD COLUMN avg_rpe NUMERIC(3,1);
ALTER TABLE exercise_performances ADD CONSTRAINT ck_exercise_performances_avg_rpe CHECK (avg_rpe BETWEEN 1 AND 10);
--rollback ALTER TABLE exercise_performances DROP COLUMN avg_rpe;
