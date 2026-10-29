package com.vegas.user.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.dto.ClaimLinkResponse;
import com.vegas.user.dto.ClaimPreviewResponse;
import com.vegas.user.dto.ClaimRequest;
import com.vegas.user.dto.MyTrainerResponse;
import com.vegas.user.entity.ClaimToken;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.event.OutboxService;
import com.vegas.user.repository.ClaimTokenRepository;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserRepository;
import com.vegas.user.security.SecureTokens;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Передача аккаунта клиенту (docs/trainer.md, шаг 4) и "мой тренер" со стороны клиента.
 *
 * Клиент с первого дня — настоящий пользователь со своим id, поэтому передача — это только
 * email + пароль для уже существующего аккаунта. Тренировки, вес и аналитика никуда не переносятся.
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    /** Сколько живёт ссылка: успеть клиенту дойти до телефона, но не валяться в переписке вечно */
    static final Duration LINK_TTL = Duration.ofDays(7);

    private final ClaimTokenRepository claimTokenRepository;
    private final TrainerClientRepository trainerClientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;
    private final AuthService authService;
    private final Clock clock;

    /** Тренер: "Передать аккаунт" — новая одноразовая ссылка (старые неиспользованные перестают работать) */
    @Transactional
    public ClaimLinkResponse createLink(UUID trainerId, UUID clientId) {
        TrainerClient link = trainerClientRepository.findByTrainerIdAndClient_IdAndEndedAtIsNull(trainerId, clientId)
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));
        if (!link.getClient().isManaged()) {
            throw new BusinessException("У клиента уже есть свой аккаунт");
        }
        claimTokenRepository.deleteUnusedByClientId(clientId);

        String raw = SecureTokens.generate();
        Instant expiresAt = clock.instant().plus(LINK_TTL);
        claimTokenRepository.save(new ClaimToken(clientId, trainerId, SecureTokens.sha256(raw), expiresAt));
        // сам токен отдаём один раз — в базе только его хеш
        return new ClaimLinkResponse(raw, expiresAt);
    }

    /** Клиент открыл ссылку: чей дневник и от какого тренера */
    @Transactional(readOnly = true)
    public ClaimPreviewResponse preview(String rawToken) {
        ClaimToken token = findValid(rawToken);
        User client = findUser(token.getClientId());
        User trainer = findUser(token.getTrainerId());
        return new ClaimPreviewResponse(client.getDisplayName(), trainer.getDisplayName(), token.getExpiresAt());
    }

    /**
     * Клиент придумал email и пароль — аккаунт его. Сразу входит (возвращаем токены, как при регистрации).
     * keepTrainer = false — связь с тренером закрывается, доступ тренера пропадает.
     */
    @Transactional
    public AuthTokens claim(String rawToken, ClaimRequest request) {
        ClaimToken token = findValid(rawToken);
        User client = findUser(token.getClientId());
        if (!client.isManaged()) {
            throw new BusinessException("Аккаунт уже передан");
        }
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Пользователь с таким email уже зарегистрирован");
        }

        Instant now = clock.instant();
        client.claim(email, passwordEncoder.encode(request.password()), now);
        token.markUsed(now);
        // notification-service узнаёт email — теперь клиенту можно слать письма
        outboxService.userRegistered(client);

        if (!request.keepTrainer()) {
            trainerClientRepository.findByClient_IdAndEndedAtIsNull(client.getId()).ifPresent(link -> endLink(link, now));
        }
        return authService.issueTokens(client);
    }

    /** Клиент: кто мой тренер (пусто — тренера нет) */
    @Transactional(readOnly = true)
    public Optional<MyTrainerResponse> myTrainer(UUID clientId) {
        return trainerClientRepository.findByClient_IdAndEndedAtIsNull(clientId)
                .map(link -> new MyTrainerResponse(link.getTrainerId(),
                        findUser(link.getTrainerId()).getDisplayName(), link.getLinkedAt()));
    }

    /** Клиент: "Отключить тренера" — тренер больше не видит и не записывает мои тренировки */
    @Transactional
    public void leaveTrainer(UUID clientId) {
        TrainerClient link = trainerClientRepository.findByClient_IdAndEndedAtIsNull(clientId)
                .orElseThrow(() -> new NotFoundException("У вас нет тренера"));
        if (link.getClient().isManaged()) {
            // ведомый клиент не может войти сам, но на всякий случай: отключать тренера может только хозяин аккаунта
            throw new ForbiddenException("Аккаунт ещё не передан клиенту");
        }
        endLink(link, clock.instant());
    }

    private void endLink(TrainerClient link, Instant at) {
        link.end(at);
        outboxService.trainerClientUnlinked(link);
    }

    /** Нет такой ссылки, уже использована или истекла — для клиента это одно и то же */
    private ClaimToken findValid(String rawToken) {
        return claimTokenRepository.findByTokenHash(SecureTokens.sha256(rawToken))
                .filter(token -> token.isValid(clock.instant()))
                .orElseThrow(() -> new NotFoundException("Ссылка недействительна или устарела — попросите тренера новую"));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }
}
