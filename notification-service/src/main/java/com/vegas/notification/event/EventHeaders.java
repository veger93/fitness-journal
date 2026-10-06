package com.vegas.notification.event;

import com.vegas.common.event.EventTypes;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

final class EventHeaders {

    private EventHeaders() {
    }

    /** Тип события из заголовка eventType или null, если заголовка нет. */
    static String eventType(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(EventTypes.HEADER);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
