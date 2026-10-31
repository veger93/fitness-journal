package com.vegas.user.service;

import com.vegas.user.dto.AuthResponse;
import com.vegas.user.entity.AuthProvider;
import com.vegas.user.entity.TrainerClient;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.event.OutboxService;
import com.vegas.user.exception.TooManyRequestsException;
import com.vegas.user.repository.TrainerClientRepository;
import com.vegas.user.repository.UserProfileRepository;
import com.vegas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private BodyWeightService bodyWeightService;
    @Mock
    private TrainerClientRepository trainerClientRepository;
    @Mock
    private OutboxService outboxService;
    @Mock
    private AuthService authService;

    private DemoService service;

    @BeforeEach
    void setUp() {
        service = new DemoService(userRepository, profileRepository, bodyWeightService, trainerClientRepository,
                outboxService, authService, new DemoRateLimiter(), CLOCK, Duration.ofHours(24), 2, 200);
        // save "присваивает" id, как база
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });
        lenient().when(trainerClientRepository.save(any(TrainerClient.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(authService.issueTokens(any())).thenReturn(new AuthTokens(AuthResponse.bearer("jwt", 900, UUID.randomUUID()), "r"));
    }

    @Test
    void athlete_demoUserWithProfileAndWeights_thenWorkoutsRequested() {
        service.start(DemoScenario.ATHLETE, "1.1.1.1");

        ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
        verify(authService).issueTokens(user.capture());
        assertThat(user.getValue().getAuthProvider()).isEqualTo(AuthProvider.DEMO);
        assertThat(user.getValue().getEmail()).isNull();
        assertThat(user.getValue().getPasswordHash()).isNull();
        assertThat(user.getValue().getDemoExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));

        ArgumentCaptor<UserProfile> profile = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileRepository).save(profile.capture());
        // онбординг пройден — гость сразу попадает на главную, а не в анкету
        assertThat(profile.getValue().isOnboardingCompleted()).isTrue();

        // 10 замеров веса раз в неделю, последний — сегодня: 79.2 кг
        verify(bodyWeightService, times(10)).upsert(eq(user.getValue().getId()), any(), any());
        verify(bodyWeightService).upsert(user.getValue().getId(), new BigDecimal("79.2"), LocalDate.of(2026, 10, 31));
        verify(outboxService).profileUpdated(user.getValue().getId());
        verify(outboxService).demoDataRequested(user.getValue().getId(), "ATHLETE", null);
    }

    @Test
    void coach_withFiveClients_eachHasLinkAndOwnScenario_recordedByCoach() {
        service.start(DemoScenario.COACH, "1.1.1.1");

        ArgumentCaptor<User> coach = ArgumentCaptor.forClass(User.class);
        verify(authService).issueTokens(coach.capture());
        assertThat(coach.getValue().isTrainer()).isTrue();
        verify(outboxService).demoDataRequested(coach.getValue().getId(), "COACH", null);

        ArgumentCaptor<TrainerClient> links = ArgumentCaptor.forClass(TrainerClient.class);
        verify(trainerClientRepository, times(5)).save(links.capture());
        verify(outboxService, times(5)).trainerClientLinked(any());
        for (TrainerClient link : links.getAllValues()) {
            assertThat(link.getTrainerId()).isEqualTo(coach.getValue().getId());
            // клиенты — без аккаунта и тоже демо: удалятся вместе с тренером
            assertThat(link.getClient().isManaged()).isTrue();
            assertThat(link.getClient().getDemoExpiresAt()).isEqualTo(coach.getValue().getDemoExpiresAt());
        }
        verify(outboxService).demoDataRequested(any(), eq("CLIENT_OVERLOAD"), eq(coach.getValue().getId()));
        verify(outboxService).demoDataRequested(any(), eq("CLIENT_AWAY"), eq(coach.getValue().getId()));
        verify(outboxService, times(6)).profileUpdated(any());
    }

    @Test
    void perIpLimit_thirdDemoFromSameIp_rejected_otherIpFine() {
        service.start(DemoScenario.ATHLETE, "1.1.1.1");
        service.start(DemoScenario.ATHLETE, "1.1.1.1");

        assertThatThrownBy(() -> service.start(DemoScenario.ATHLETE, "1.1.1.1"))
                .isInstanceOf(TooManyRequestsException.class);
        service.start(DemoScenario.ATHLETE, "2.2.2.2");
        verify(authService, times(3)).issueTokens(any());
    }

    @Test
    void tooManyActiveDemos_rejected_withoutUsingIpQuota() {
        when(userRepository.countByAuthProviderAndDemoExpiresAtAfter(AuthProvider.DEMO, NOW)).thenReturn(200L);

        assertThatThrownBy(() -> service.start(DemoScenario.ATHLETE, "1.1.1.1"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(userRepository, never()).save(any());
        verify(outboxService, never()).demoDataRequested(any(), anyString(), isNull());
    }
}
