package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.event.OutboxService;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private TrainerClientRepository trainerClientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OutboxService outboxService;

    private AccountDeletionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionService(userRepository, trainerClientRepository, passwordEncoder, outboxService);
        user = withId(User.local("coach@mail.ru", "hash", "Сергей"), UUID.randomUUID());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void wrongPassword_400_nothingDeleted() {
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteAccount(user.getId(), "wrong"))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).delete(any());
        verify(outboxService, never()).userDeleted(any());
    }

    @Test
    void regularUser_deletedAndEventSent() {
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(trainerClientRepository.findAllByTrainerIdAndEndedAtIsNullOrderByLinkedAtDesc(user.getId()))
                .thenReturn(List.of());

        service.deleteAccount(user.getId(), "secret");

        verify(userRepository).delete(user);
        verify(outboxService).userDeleted(user.getId());
    }

    @Test
    void trainer_managedClientsDeleted_ownAccountClientsKept() {
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        User managed = withId(User.managed("Иван"), UUID.randomUUID());
        User claimed = withId(User.managed("Пётр"), UUID.randomUUID());
        claimed.claim("petr@mail.ru", "hash2", NOW);
        when(trainerClientRepository.findAllByTrainerIdAndEndedAtIsNullOrderByLinkedAtDesc(user.getId()))
                .thenReturn(List.of(new TrainerClient(user.getId(), managed, null, NOW),
                        new TrainerClient(user.getId(), claimed, null, NOW)));

        service.deleteAccount(user.getId(), "secret");

        verify(userRepository).delete(managed);
        verify(outboxService).userDeleted(managed.getId());
        // у клиента свой аккаунт — он остаётся; связь с тренером удалит база (ON DELETE CASCADE)
        verify(userRepository, never()).delete(claimed);
        verify(outboxService, never()).userDeleted(claimed.getId());
        verify(userRepository).delete(user);
        verify(outboxService).userDeleted(user.getId());
    }

    private static User withId(User user, UUID id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
