package com.vegas.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * gateway-service: принимает все запросы фронта и проксирует их в нужный сервис.
 * Позже здесь же будет проверка JWT (один раз на входе, а не в каждом сервисе).
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
