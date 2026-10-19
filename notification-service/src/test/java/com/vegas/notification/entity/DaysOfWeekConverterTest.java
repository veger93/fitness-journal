package com.vegas.notification.entity;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DaysOfWeekConverterTest {

    private final DaysOfWeekConverter converter = new DaysOfWeekConverter();

    @Test
    void roundTrip_sortedByWeekOrder() {
        String column = converter.convertToDatabaseColumn(Set.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY));

        assertThat(column).isEqualTo("MONDAY,FRIDAY");
        assertThat(converter.convertToEntityAttribute(column)).containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void emptyAndNull() {
        assertThat(converter.convertToDatabaseColumn(Set.of())).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }
}
