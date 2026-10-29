package com.vegas.user.service;

import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.ForbiddenException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.dto.ClientResponse;
import com.vegas.user.dto.CreateClientRequest;
import com.vegas.user.dto.UpdateClientRequest;
import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.ExperienceLevel;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.event.OutboxService;
import com.vegas.user.mapper.UserMapper;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserProfileRepository;
import com.vegas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainerServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private TrainerClientRepository trainerClientRepository;
    @Mock
    private BodyWeightService bodyWeightService;
    @Mock
    private OutboxService outboxService;

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private TrainerService trainerService;
    private User trainer;
    private final UUID trainerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        trainerService = new TrainerService(userRepository, profileRepository, trainerClientRepository,
                bodyWeightService, outboxService, userMapper, CLOCK);
        trainer = withId(User.local("coach@mail.ru", "hash", "Сергей"), trainerId);
    }

    @Test
    void createClient_createsManagedUserWithLinkAndEvents() {
        trainer.setTrainer(true);
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        // save возвращает пользователя уже с id — как настоящий репозиторий
        when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0), UUID.randomUUID()));
        when(trainerClientRepository.save(any(TrainerClient.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse response = trainerService.createClient(trainerId, new CreateClientRequest(
                "  Иван ", "+7 900 000-00-00", 180, ExperienceLevel.BEGINNER, new BigDecimal("82.5")));

        ArgumentCaptor<User> client = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(client.capture());
        // ведомый: ни email, ни пароля — войти нельзя
        assertThat(client.getValue().getAuthProvider()).isEqualTo(AuthProvider.NONE);
        assertThat(client.getValue().getEmail()).isNull();
        assertThat(client.getValue().getPasswordHash()).isNull();

        ArgumentCaptor<UserProfile> profile = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profile.capture());
        assertThat(profile.getValue().getHeightCm()).isEqualTo((short) 180);
        assertThat(profile.getValue().isOnboardingCompleted()).isFalse();

        verify(bodyWeightService).upsert(response.id(), new BigDecimal("82.5"), LocalDate.of(2026, 10, 30));
        verify(outboxService).profileUpdated(response.id());
        verify(outboxService).trainerClientLinked(any(TrainerClient.class));

        assertThat(response.displayName()).isEqualTo("Иван");
        assertThat(response.managed()).isTrue();
        assertThat(response.phone()).isEqualTo("+7 900 000-00-00");
    }

    @Test
    void createClient_onlyNameRequired_noWeightRecorded() {
        trainer.setTrainer(true);
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0), UUID.randomUUID()));
        when(trainerClientRepository.save(any(TrainerClient.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse response = trainerService.createClient(trainerId,
                new CreateClientRequest("Иван", " ", null, null, null));

        verify(bodyWeightService, never()).upsert(any(), any(), any());
        assertThat(response.phone()).isNull();
        assertThat(response.heightCm()).isNull();
    }

    @Test
    void createClient_withoutTrainerMode_forbidden() {
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

        assertThatThrownBy(() -> trainerService.createClient(trainerId,
                new CreateClientRequest("Иван", null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getClient_notMyClient_notFound() {
        trainer.setTrainer(true);
        UUID strangerId = UUID.randomUUID();
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(trainerClientRepository.findByTrainerIdAndClient_IdAndEndedAtIsNull(trainerId, strangerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trainerService.getClient(trainerId, strangerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateClient_renameManagedClient() {
        trainer.setTrainer(true);
        User client = withId(User.managed("Иван"), UUID.randomUUID());
        TrainerClient link = new TrainerClient(trainerId, client, null, CLOCK.instant());
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(trainerClientRepository.findByTrainerIdAndClient_IdAndEndedAtIsNull(trainerId, client.getId()))
                .thenReturn(Optional.of(link));

        ClientResponse response = trainerService.updateClient(trainerId, client.getId(),
                new UpdateClientRequest("Иван Петров", "8 900 1112233"));

        assertThat(response.displayName()).isEqualTo("Иван Петров");
        assertThat(response.phone()).isEqualTo("8 900 1112233");
    }

    @Test
    void updateClient_renameClientWithOwnAccount_forbidden() {
        trainer.setTrainer(true);
        User client = withId(User.local("ivan@mail.ru", "hash", "Иван"), UUID.randomUUID());
        TrainerClient link = new TrainerClient(trainerId, client, null, CLOCK.instant());
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(trainerClientRepository.findByTrainerIdAndClient_IdAndEndedAtIsNull(trainerId, client.getId()))
                .thenReturn(Optional.of(link));

        assertThatThrownBy(() -> trainerService.updateClient(trainerId, client.getId(),
                new UpdateClientRequest("Ванька", null)))
                .isInstanceOf(ForbiddenException.class);
        assertThat(client.getDisplayName()).isEqualTo("Иван");
    }

    @Test
    void disableTrainerMode_withActiveClients_rejected() {
        trainer.setTrainer(true);
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));
        when(trainerClientRepository.existsByTrainerIdAndEndedAtIsNull(trainerId)).thenReturn(true);

        assertThatThrownBy(() -> trainerService.setTrainerMode(trainerId, false))
                .isInstanceOf(BusinessException.class);
        assertThat(trainer.isTrainer()).isTrue();
    }

    @Test
    void enableTrainerMode() {
        when(userRepository.findById(trainerId)).thenReturn(Optional.of(trainer));

        assertThat(trainerService.setTrainerMode(trainerId, true).trainer()).isTrue();
    }

    /** id генерирует база, а в unit-тесте базы нет — ставим id через reflection */
    private static User withId(User user, UUID id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
