--liquibase formatted sql

--changeset sergey:007-trainer-notifications
-- Уведомления тренеру о его клиентах (рекорд, плато, перегрузка клиента): docs/trainer.md, шаг 5.2.
-- client_id и client_name — о ком уведомление: тап в ленте открывает дневник этого клиента.
-- У обычных уведомлений оба NULL.
ALTER TABLE notifications ADD COLUMN client_id UUID;
ALTER TABLE notifications ADD COLUMN client_name VARCHAR(100);

-- IN_APP: только в ленте приложения, без письма — клиентов у тренера десятки, письмо на каждый рекорд было бы спамом
ALTER TABLE notifications DROP CONSTRAINT ck_notifications_status;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_status CHECK (status IN ('SENT', 'SKIPPED', 'IN_APP'));
--rollback ALTER TABLE notifications DROP CONSTRAINT ck_notifications_status; ALTER TABLE notifications ADD CONSTRAINT ck_notifications_status CHECK (status IN ('SENT', 'SKIPPED')); ALTER TABLE notifications DROP COLUMN client_name; ALTER TABLE notifications DROP COLUMN client_id;
