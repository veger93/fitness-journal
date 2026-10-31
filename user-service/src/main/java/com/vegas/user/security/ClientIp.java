package com.vegas.user.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP посетителя за прокси. Запрос идёт браузер -> Caddy -> gateway -> user-service, поэтому
 * request.getRemoteAddr() — это адрес gateway. Caddy пишет настоящий адрес клиента в X-Forwarded-For
 * (присланный клиентом заголовок он не принимает — подделать нельзя), gateway дописывает свой через запятую.
 * Значит, нужен ПЕРВЫЙ адрес в списке.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
