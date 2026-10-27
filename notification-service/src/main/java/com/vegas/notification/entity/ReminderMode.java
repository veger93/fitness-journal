package com.vegas.notification.entity;

/** Откуда брать дни напоминаний. */
public enum ReminderMode {
    /** из плана на неделю: в дни отдыха напоминаний нет, в письме — название комплекса */
    PLAN,
    /** дни выбраны вручную */
    CUSTOM
}
