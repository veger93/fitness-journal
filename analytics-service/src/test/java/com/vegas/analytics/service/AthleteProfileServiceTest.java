package com.vegas.analytics.service;

import com.vegas.analytics.entity.AthleteProfile;
import com.vegas.analytics.repository.AthleteProfileRepository;
import com.vegas.common.event.UserProfileUpdatedEvent;
import com.vegas.common.model.Gender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AthleteProfileServiceTest {

    @Mock
    private AthleteProfileRepository profileRepository;

    @InjectMocks
    private AthleteProfileService service;

    private final UUID userId = UUID.randomUUID();

    @Test
    void firstEvent_createsProfile() {
        service.apply(event("INTERMEDIATE", "2026-10-01T10:00:00Z"));

        ArgumentCaptor<AthleteProfile> saved = ArgumentCaptor.forClass(AthleteProfile.class);
        verify(profileRepository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getExperienceLevel()).isEqualTo("INTERMEDIATE");
    }

    @Test
    void newerEvent_updates() {
        AthleteProfile existing = AthleteProfile.from(event("BEGINNER", "2026-10-01T10:00:00Z"));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(existing));

        service.apply(event("INTERMEDIATE", "2026-10-02T10:00:00Z"));

        assertThat(existing.getExperienceLevel()).isEqualTo("INTERMEDIATE");
    }

    @Test
    void olderOrRepeatedEvent_isIgnored() {
        AthleteProfile existing = AthleteProfile.from(event("ADVANCED", "2026-10-02T10:00:00Z"));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(existing));

        service.apply(event("BEGINNER", "2026-10-01T10:00:00Z"));   // пришло не по порядку
        service.apply(event("BEGINNER", "2026-10-02T10:00:00Z"));   // то же время — повтор

        assertThat(existing.getExperienceLevel()).isEqualTo("ADVANCED");
    }

    private UserProfileUpdatedEvent event(String level, String updatedAt) {
        return new UserProfileUpdatedEvent(userId, Gender.MALE, 1995, 178, level,
                new BigDecimal("80.5"), Instant.parse(updatedAt));
    }
}
