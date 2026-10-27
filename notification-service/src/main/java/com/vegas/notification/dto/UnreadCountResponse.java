package com.vegas.notification.dto;

/** Число на колокольчике в шапке. Объект, а не голое число: в ответ потом легко добавить поле, не ломая фронт. */
public record UnreadCountResponse(long count) {
}
