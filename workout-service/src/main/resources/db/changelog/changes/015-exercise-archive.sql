--liquibase formatted sql

--changeset sergey:015-exercise-archive
-- Удаление своего упражнения. Если по нему есть тренировки или оно стоит в комплексе — физически удалить нельзя
-- (сломается история и графики), поэтому прячем: archived_at != null -> в каталоге и поиске не показываем.
ALTER TABLE exercises ADD COLUMN archived_at TIMESTAMPTZ;
--rollback ALTER TABLE exercises DROP COLUMN archived_at;
