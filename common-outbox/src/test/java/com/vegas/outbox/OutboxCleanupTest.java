package com.vegas.outbox;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxCleanupTest {

    @Test
    void deletesPublishedOlderThanRetention() {
        Instant now = Instant.parse("2026-10-31T12:00:00Z");
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        when(repository.deletePublishedBefore(now.minus(Duration.ofDays(7)))).thenReturn(42);

        int deleted = new OutboxCleanup(repository, Clock.fixed(now, ZoneOffset.UTC), Duration.ofDays(7)).deleteOldPublished();

        assertThat(deleted).isEqualTo(42);
        verify(repository).deletePublishedBefore(Instant.parse("2026-10-24T12:00:00Z"));
    }
}
