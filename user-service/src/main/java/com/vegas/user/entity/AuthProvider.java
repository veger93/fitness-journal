package com.vegas.user.entity;

/** Как пользователь зарегистрировался. */
public enum AuthProvider {
    LOCAL,   // email + пароль
    GOOGLE,  // OAuth
    NONE,    // ведомый клиент тренера: входа нет, пока тренер не передаст аккаунт (docs/trainer.md)
    DEMO     // демо "посмотреть изнутри": без email и пароля, живёт сутки (docs/demo.md)
}
