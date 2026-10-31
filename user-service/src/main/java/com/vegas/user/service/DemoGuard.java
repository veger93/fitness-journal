package com.vegas.user.service;

import com.vegas.common.exception.ForbiddenException;
import com.vegas.user.entity.User;

/**
 * Что нельзя в демо (docs/demo.md): всё, что касается почты, настоящего аккаунта и передачи аккаунта.
 * Фронт эти кнопки прячет; проверка на сервере — на случай прямого запроса.
 */
final class DemoGuard {

    static final String MESSAGE = "В демо это недоступно — зарегистрируйтесь, чтобы вести свой дневник";

    private DemoGuard() {
    }

    static void requireNotDemo(User user) {
        if (user.isDemo()) {
            throw new ForbiddenException(MESSAGE);
        }
    }
}
