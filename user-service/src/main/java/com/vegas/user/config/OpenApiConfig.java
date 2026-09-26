package com.vegas.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Описание API для Swagger UI (http://localhost:8081/swagger-ui.html).
 * SecurityScheme добавляет кнопку "Authorize": вставляешь туда accessToken —
 * и Swagger сам подставляет заголовок Authorization во все запросы.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "user-service API", version = "v1"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
