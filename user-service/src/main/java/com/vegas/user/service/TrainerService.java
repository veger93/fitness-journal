package com.vegas.user.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.dto.ClientResponse;
import com.vegas.user.dto.CreateClientRequest;
import com.vegas.user.dto.UpdateClientRequest;
import com.vegas.user.dto.UserResponse;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.event.OutboxService;
import com.vegas.user.mapper.UserMapper;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserProfileRepository;
import com.vegas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Режим тренера и его клиенты (docs/trainer.md, шаг 1).
 *
 * Правило доступа: тренер видит клиента, только пока у них есть АКТИВНАЯ связь в trainer_clients.
 * Чужой или ушедший клиент для тренера "не найден" (404, а не 403): так нельзя даже проверить,
 * существует ли пользователь с таким id.
 */
@Service
@RequiredArgsConstructor
public class TrainerService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final TrainerClientRepository trainerClientRepository;
    private final BodyWeightService bodyWeightService;
    private final OutboxService outboxService;
    private final UserMapper userMapper;
    private final Clock clock;

    /** Переключатель "Я тренер" в профиле */
    @Transactional
    public UserResponse setTrainerMode(UUID userId, boolean enabled) {
        User user = findUser(userId);
        if (!enabled && trainerClientRepository.existsByTrainerIdAndEndedAtIsNull(userId)) {
            // иначе клиенты "повиснут": тренер их не видит, а сами они войти не могут
            throw new BusinessException("Нельзя выключить режим тренера, пока у вас есть клиенты");
        }
        user.setTrainer(enabled);
        return userMapper.toResponse(user);
    }

    /**
     * Тренер заводит клиента: ведомый пользователь (без входа) + профиль + связь.
     * Всё в одной транзакции: или клиент создан целиком, или ничего.
     */
    @Transactional
    public ClientResponse createClient(UUID trainerId, CreateClientRequest request) {
        requireTrainer(trainerId);

        User client = userRepository.save(User.managed(request.displayName().trim()));

        UserProfile profile = new UserProfile(client);
        profile.setHeightCm(request.heightCm() == null ? null : request.heightCm().shortValue());
        profile.setExperienceLevel(request.experienceLevel());
        // онбординг не завершён: пол и год рождения клиент заполнит сам, когда получит аккаунт
        profileRepository.save(profile);

        if (request.weightKg() != null) {
            bodyWeightService.upsert(client.getId(), request.weightKg(), LocalDate.now(clock));
        }

        TrainerClient link = trainerClientRepository.save(
                new TrainerClient(trainerId, client, blankToNull(request.phone()), clock.instant()));

        // аналитике — уровень и вес клиента; workout/analytics — копия связи для проверки доступа тренера
        outboxService.profileUpdated(client.getId());
        outboxService.trainerClientLinked(link);

        return toResponse(link, profile, request.weightKg());
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> listClients(UUID trainerId) {
        requireTrainer(trainerId);
        // клиентов у тренера десятки, поэтому профиль и вес каждого — отдельными простыми запросами
        return trainerClientRepository.findAllByTrainerIdAndEndedAtIsNullOrderByLinkedAtDesc(trainerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse getClient(UUID trainerId, UUID clientId) {
        requireTrainer(trainerId);
        return toResponse(findLink(trainerId, clientId));
    }

    @Transactional
    public ClientResponse updateClient(UUID trainerId, UUID clientId, UpdateClientRequest request) {
        requireTrainer(trainerId);
        TrainerClient link = findLink(trainerId, clientId);
        User client = link.getClient();

        String newName = request.displayName().trim();
        if (!newName.equals(client.getDisplayName())) {
            if (!client.isManaged()) {
                throw new ForbiddenException("Имя меняет сам клиент: у него свой аккаунт");
            }
            client.setDisplayName(newName);
        }
        link.setPhone(blankToNull(request.phone()));
        return toResponse(link);
    }

    /**
     * Убрать клиента из списка.
     * - Клиент без аккаунта (ведомый): его данные больше никому не нужны — удаляем пользователя целиком,
     *   а событие UserDeleted просит остальные сервисы удалить его тренировки, аналитику, уведомления.
     * - Клиент со своим аккаунтом: только закрываем связь — аккаунт и данные остаются у него.
     *
     * @return true — клиент удалён полностью, false — только отвязан
     */
    @Transactional
    public boolean removeClient(UUID trainerId, UUID clientId) {
        requireTrainer(trainerId);
        TrainerClient link = findLink(trainerId, clientId);
        User client = link.getClient();
        if (client.isManaged()) {
            // ON DELETE CASCADE в базе удалит профиль, вес, связь и ссылки передачи
            userRepository.delete(client);
            outboxService.userDeleted(clientId);
            return true;
        }
        link.end(clock.instant());
        outboxService.trainerClientUnlinked(link);
        return false;
    }

    private void requireTrainer(UUID userId) {
        if (!findUser(userId).isTrainer()) {
            throw new ForbiddenException("Включите режим тренера в профиле");
        }
    }

    private TrainerClient findLink(UUID trainerId, UUID clientId) {
        return trainerClientRepository.findByTrainerIdAndClient_IdAndEndedAtIsNull(trainerId, clientId)
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private ClientResponse toResponse(TrainerClient link) {
        UUID clientId = link.getClient().getId();
        UserProfile profile = profileRepository.findById(clientId).orElse(null);
        BigDecimal weight = bodyWeightService.currentWeight(clientId).orElse(null);
        return toResponse(link, profile, weight);
    }

    private static ClientResponse toResponse(TrainerClient link, UserProfile profile, BigDecimal weight) {
        User client = link.getClient();
        return new ClientResponse(
                client.getId(),
                client.getDisplayName(),
                link.getPhone(),
                client.isManaged(),
                profile == null ? null : profile.getHeightCm(),
                profile == null ? null : profile.getExperienceLevel(),
                weight,
                link.getLinkedAt()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
