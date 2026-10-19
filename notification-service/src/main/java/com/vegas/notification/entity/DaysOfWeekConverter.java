package com.vegas.notification.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AttributeConverter: как хранить поле, для которого нет "родного" типа в БД.
 * В Java — Set<DayOfWeek>, в колонке — строка "MONDAY,WEDNESDAY,FRIDAY".
 */
@Converter
public class DaysOfWeekConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return null;
        }
        return days.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
