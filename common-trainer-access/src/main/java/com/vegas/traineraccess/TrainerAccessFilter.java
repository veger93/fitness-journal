package com.vegas.traineraccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegas.common.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * "Тренер работает за клиента". Запрос с заголовком X-Client-Id: <id клиента> выполняется так,
 * будто его сделал сам клиент: все существующие эндпоинты работают без изменений.
 *
 * Как: после проверки токена (BearerTokenAuthenticationFilter) подменяем пользователя в SecurityContext —
 * Jwt с sub = клиент, а в стандартном поле "act" (actor, RFC 8693) остаётся, кто на самом деле действует — тренер.
 * Пустить можно, только если в trainer_links есть связь "этот клиент — этот тренер", иначе 403.
 *
 * Не @Component специально: Spring Boot сам добавил бы любой бин-фильтр в общую цепочку фильтров сервлета,
 * и он сработал бы второй раз и не в том месте. Сервис создаёт фильтр в своём SecurityConfig.
 */
public class TrainerAccessFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Client-Id";
    /** Поле токена, где лежит тот, кто действует за клиента */
    public static final String ACTOR_CLAIM = "act";

    private final TrainerLinkRepository repository;
    private final ObjectMapper objectMapper;

    public TrainerAccessFilter(TrainerLinkRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // нет заголовка — обычный запрос "за себя"; нет токена — дальше Spring Security ответит 401
        if (header == null || !(auth instanceof JwtAuthenticationToken jwtAuth)) {
            chain.doFilter(request, response);
            return;
        }

        UUID clientId;
        try {
            clientId = UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            reject(request, response, HttpStatus.BAD_REQUEST, "Некорректный " + HEADER);
            return;
        }

        Jwt jwt = jwtAuth.getToken();
        UUID trainerId = UUID.fromString(jwt.getSubject());
        if (!repository.existsByClientIdAndTrainerId(clientId, trainerId)) {
            reject(request, response, HttpStatus.FORBIDDEN, "Нет доступа к этому клиенту");
            return;
        }

        Jwt actingAsClient = Jwt.withTokenValue(jwt.getTokenValue())
                .headers(headers -> headers.putAll(jwt.getHeaders()))
                .claims(claims -> {
                    claims.putAll(jwt.getClaims());
                    claims.put("sub", clientId.toString());
                    claims.put(ACTOR_CLAIM, Map.of("sub", trainerId.toString()));
                })
                .build();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(actingAsClient, jwtAuth.getAuthorities()));
        SecurityContextHolder.setContext(context);

        chain.doFilter(request, response);
    }

    /** Кто на самом деле сделал запрос: тренер, если он работает за клиента, иначе null (сам пользователь) */
    public static UUID actorId(Jwt jwt) {
        if (jwt.getClaim(ACTOR_CLAIM) instanceof Map<?, ?> actor && actor.get("sub") instanceof String sub) {
            return UUID.fromString(sub);
        }
        return null;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ErrorResponse.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
