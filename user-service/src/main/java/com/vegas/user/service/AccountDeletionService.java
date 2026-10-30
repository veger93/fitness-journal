package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.event.OutboxService;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * "Удалить мой аккаунт" (docs/account.md, шаг 2).
 *
 * В user-service удаляем строку users — остальное (профиль, вес, refresh-токены, коды, связи с тренером,
 * ссылки передачи) удалит база по ON DELETE CASCADE. Тренировки, аналитика и уведомления живут в других сервисах:
 * туда уходит событие UserDeleted (через outbox, в той же транзакции), и каждый сервис чистит своё (UserDataCleaner).
 *
 * Тренер: клиенты без аккаунта (ведомые) существуют только благодаря ему — удаляются вместе с ним.
 * Клиенты со своим аккаунтом остаются: у них просто пропадает тренер.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final TrainerClientRepository trainerClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final OutboxService outboxService;

    @Transactional
    public void deleteAccount(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            // 400, а не 401: 401 фронт понимает как "сессия кончилась" и выкидывает на вход
            throw new BadRequestException("Неверный пароль");
        }

        List<User> managedClients = trainerClientRepository.findAllByTrainerIdAndEndedAtIsNullOrderByLinkedAtDesc(userId)
                .stream()
                .map(TrainerClient::getClient)
                .filter(User::isManaged)
                .toList();
        for (User client : managedClients) {
            userRepository.delete(client);
            outboxService.userDeleted(client.getId());
        }

        userRepository.delete(user);
        outboxService.userDeleted(userId);
        log.info("User {} deleted the account (with {} managed clients)", userId, managedClients.size());
    }
}
