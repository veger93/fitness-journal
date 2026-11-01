package com.vegas.user.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Не больше N демо в час с одного IP (docs/demo.md). Скользящее окно: храним время последних запусков.
 *
 * В памяти, а не в базе: сервис один, а после перезапуска счётчик просто начнётся заново — для защиты
 * от случайного или ботового "накликивания" этого достаточно. Сам IP никуда не записываем.
 * synchronized: запросы приходят из разных потоков, а HashMap и ArrayDeque не потокобезопасны;
 * демо создают редко — очереди на блокировке не будет.
 */
@Component
public class DemoRateLimiter {

    static final Duration WINDOW = Duration.ofHours(1);
    /** Когда адресов накопится столько, выбросим те, у кого все запуски старше окна (чтобы память не росла) */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Deque<Instant>> starts = new HashMap<>();

    /** true — можно запускать (и запуск засчитан), false — лимит исчерпан */
    public synchronized boolean tryAcquire(String ip, int limit, Instant now) {
        Instant from = now.minus(WINDOW);
        if (starts.size() > CLEANUP_THRESHOLD) {
            starts.values().removeIf(times -> times.isEmpty() || times.peekLast().isBefore(from));
        }
        Deque<Instant> times = starts.computeIfAbsent(ip, key -> new ArrayDeque<>());
        while (!times.isEmpty() && times.peekFirst().isBefore(from)) {
            times.pollFirst();
        }
        if (times.size() >= limit) {
            return false;
        }
        times.addLast(now);
        return true;
    }
}
