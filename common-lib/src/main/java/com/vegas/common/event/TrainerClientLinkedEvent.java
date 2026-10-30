package com.vegas.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * "У клиента появился тренер". Сервисы с данными клиента (тренировки, аналитика) хранят копию связей у себя
 * и по ней проверяют, можно ли тренеру читать и записывать за клиента — без запроса в user-service
 * (docs/trainer.md, "Доступ тренера к данным клиента"). Ключ сообщения — clientId.
 *
 * clientName — имя клиента для уведомлений тренеру ("Ольга: новый рекорд"). Событие приходит повторно,
 * когда тренер переименовал клиента: копии связей просто перезаписывают строку.
 * В старых событиях поля нет — Jackson оставит null.
 */
public record TrainerClientLinkedEvent(
        UUID trainerId,
        UUID clientId,
        Instant linkedAt,
        String clientName
) {
}
