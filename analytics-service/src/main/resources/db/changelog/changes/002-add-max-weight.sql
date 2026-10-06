--liquibase formatted sql

--changeset sergey:002-add-max-weight
-- Самый тяжёлый рабочий подход (для рекорда веса).
-- Отличается от best_weight_kg: там вес подхода с лучшим 1ПМ.
-- Пример: 80×8 (1ПМ 101) и 85×5 (1ПМ 99) -> best_weight_kg = 80, max_weight_kg = 85.
ALTER TABLE exercise_performances ADD COLUMN max_weight_kg   NUMERIC(6,2);
ALTER TABLE exercise_performances ADD COLUMN max_weight_reps INTEGER;

-- Заполняем уже существующие строки (backfill). Точного значения для старых данных у нас нет,
-- поэтому берём ближайшее приближение — вес лучшего по 1ПМ подхода.
-- Новые события будут считаться точно.
UPDATE exercise_performances
SET max_weight_kg   = best_weight_kg,
    max_weight_reps = best_reps
WHERE best_weight_kg IS NOT NULL;
--rollback ALTER TABLE exercise_performances DROP COLUMN max_weight_reps; ALTER TABLE exercise_performances DROP COLUMN max_weight_kg;
