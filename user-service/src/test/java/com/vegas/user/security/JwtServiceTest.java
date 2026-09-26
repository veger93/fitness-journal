package com.vegas.user.security;

import com.vegas.user.config.JwtProperties;
import com.vegas.user.entity.Role;
import com.vegas.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Обычный unit-тест: без Spring-контекста и без БД, запускается за миллисекунды.
 * JwtService создаём руками через new.
 */
class JwtServiceTest {

    // тестовый ключ (base64, 512 бит) — не совпадает с ключом из application-local.yml
    private static final String SECRET =
            "7Qyf2vYB4aftUkypMXD8pXYd8FCq2xlYKGJ3/Z9HNaUqtPCEtEOjtRaq3XdPWMDVj2EAI5EAfLLo3o3cwKDt8w==";
    private static final String OTHER_SECRET =
            "0EJXS5DoMcGPf1mXDuHC7Fn/xZnsIq1q0iyBJXy1pz3pRxx5FNGNmQ+Qihz2H0rix/fZ2V0yopKjXGdQci97MQ==";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));

    @Test
    void generatedToken_isParsedBackWithSameUser() {
        User user = userWithId();

        String token = jwtService.generateAccessToken(user);
        Optional<AuthenticatedUser> parsed = jwtService.parse(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().id()).isEqualTo(user.getId());
        assertThat(parsed.get().email()).isEqualTo("ivan@mail.ru");
        assertThat(parsed.get().role()).isEqualTo(Role.USER);
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwtService.generateAccessToken(userWithId());

        assertThat(jwtService.parse(token + "x")).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        JwtService otherService = new JwtService(new JwtProperties(OTHER_SECRET, Duration.ofHours(1)));
        String foreignToken = otherService.generateAccessToken(userWithId());

        assertThat(jwtService.parse(foreignToken)).isEmpty();
    }

    @Test
    void expiredToken_isRejected() {
        // отрицательный TTL -> токен просрочен в момент выпуска
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-1)));
        String token = shortLived.generateAccessToken(userWithId());

        assertThat(jwtService.parse(token)).isEmpty();
    }

    @Test
    void garbage_isRejected() {
        assertThat(jwtService.parse("not-a-jwt")).isEmpty();
    }

    /** id обычно проставляет Hibernate при сохранении; в unit-тесте БД нет — ставим через reflection. */
    private static User userWithId() {
        User user = User.local("ivan@mail.ru", "hash", "Иван");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
