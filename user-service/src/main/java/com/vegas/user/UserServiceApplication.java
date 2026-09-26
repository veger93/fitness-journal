package com.vegas.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * user-service: регистрация, логин, JWT, OAuth (Google), профиль и физ. данные (пол, рост, история веса).
 *
 * UserDetailsServiceAutoConfiguration исключаем: без этого Spring Security создаёт
 * тестового пользователя "user" со случайным паролем в логах. Нам он не нужен —
 * пользователи живут в БД, а аутентификация идёт по JWT.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
