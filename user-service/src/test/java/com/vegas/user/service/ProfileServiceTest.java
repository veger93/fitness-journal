package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.model.Gender;
import com.vegas.user.dto.ProfileResponse;
import com.vegas.user.dto.UpdateProfileRequest;
import com.vegas.user.entity.ExperienceLevel;
import com.vegas.user.entity.User;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.event.OutboxService;
import com.vegas.user.mapper.ProfileMapper;
import com.vegas.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class ProfileServiceTest {

    // "сегодня" в тестах всегда 27.09.2026 — тесты не сломаются в новом году
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);

    @Mock
    private UserProfileRepository profileRepository;
    @Mock
    private BodyWeightService bodyWeightService;
    @Mock
    private OutboxService outboxService;

    // настоящий маппер (сгенерированный MapStruct), мокать его незачем
    private final ProfileMapper profileMapper = Mappers.getMapper(ProfileMapper.class);

    private ProfileService profileService;
    private final UUID userId = UUID.randomUUID();
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        // Clock не мок, поэтому собираем сервис вручную, а не через @InjectMocks
        profileService = new ProfileService(profileRepository, bodyWeightService, profileMapper, CLOCK, outboxService);
        profile = new UserProfile(User.local("ivan@mail.ru", "hash", "Иван"));
    }

    @Test
    void updateProfile_fillsProfileCompletesOnboardingAndRecordsWeight() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(bodyWeightService.currentWeight(userId)).thenReturn(Optional.empty()); // веса ещё не было

        ProfileResponse response = profileService.updateProfile(userId, request(1995, "80.5"));

        assertThat(profile.getGender()).isEqualTo(Gender.MALE);
        assertThat(profile.getBirthYear()).isEqualTo((short) 1995);
        assertThat(profile.getHeightCm()).isEqualTo((short) 178);
        assertThat(profile.isOnboardingCompleted()).isTrue();
        verify(bodyWeightService).upsert(userId, new BigDecimal("80.5"), TODAY);
        verify(outboxService).profileUpdated(userId); // аналитика узнает об изменении

        assertThat(response.onboardingCompleted()).isTrue();
        assertThat(response.currentWeightKg()).isEqualByComparingTo("80.5");
    }

    @Test
    void updateProfile_sameWeightWithDifferentScale_doesNotCreateNewEntry() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(bodyWeightService.currentWeight(userId)).thenReturn(Optional.of(new BigDecimal("80.00")));

        profileService.updateProfile(userId, request(1995, "80.0")); // 80.0 и 80.00 — один и тот же вес

        verify(bodyWeightService, never()).upsert(any(), any(), any());
    }

    @Test
    void updateProfile_tooYoung_throwsBadRequest() {
        assertThatThrownBy(() -> profileService.updateProfile(userId, request(2020, "40")))
                .isInstanceOf(BadRequestException.class);

        verify(profileRepository, never()).findById(any());
    }

    @Test
    void getProfile_beforeOnboarding_returnsEmptyProfile() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(bodyWeightService.currentWeight(userId)).thenReturn(Optional.empty());

        ProfileResponse response = profileService.getProfile(userId);

        assertThat(response.onboardingCompleted()).isFalse();
        assertThat(response.currentWeightKg()).isNull();
    }

    private static UpdateProfileRequest request(int birthYear, String weight) {
        return new UpdateProfileRequest(Gender.MALE, birthYear, 178, ExperienceLevel.INTERMEDIATE, new BigDecimal(weight));
    }
}
