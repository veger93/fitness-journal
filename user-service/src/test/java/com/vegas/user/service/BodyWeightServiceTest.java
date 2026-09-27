package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.user.dto.RecordWeightRequest;
import com.vegas.user.dto.WeightEntryResponse;
import com.vegas.user.entity.BodyWeightEntry;
import com.vegas.user.entity.User;
import com.vegas.user.mapper.WeightMapper;
import com.vegas.user.repository.BodyWeightEntryRepository;
import com.vegas.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
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
class BodyWeightServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);

    @Mock
    private BodyWeightEntryRepository weightRepository;
    @Mock
    private UserRepository userRepository;

    private final WeightMapper weightMapper = Mappers.getMapper(WeightMapper.class);

    private BodyWeightService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BodyWeightService(weightRepository, userRepository, weightMapper, CLOCK);
    }

    @Test
    void record_newDay_createsEntry() {
        User user = User.local("ivan@mail.ru", "hash", "Иван");
        when(weightRepository.findByUserIdAndMeasuredOn(userId, TODAY)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(weightRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0)); // вернуть то, что сохраняли

        WeightEntryResponse response = service.record(userId, TODAY, new RecordWeightRequest(new BigDecimal("81.2")));

        ArgumentCaptor<BodyWeightEntry> saved = ArgumentCaptor.forClass(BodyWeightEntry.class);
        verify(weightRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getMeasuredOn()).isEqualTo(TODAY);
        assertThat(response.weightKg()).isEqualByComparingTo("81.2");
    }

    @Test
    void record_sameDayAgain_updatesExistingEntryWithoutSave() {
        BodyWeightEntry existing = new BodyWeightEntry(User.local("ivan@mail.ru", "hash", "Иван"), new BigDecimal("80.0"), TODAY);
        when(weightRepository.findByUserIdAndMeasuredOn(userId, TODAY)).thenReturn(Optional.of(existing));

        service.record(userId, TODAY, new RecordWeightRequest(new BigDecimal("79.6")));

        assertThat(existing.getWeightKg()).isEqualByComparingTo("79.6");
        verify(weightRepository, never()).save(any()); // UPDATE сделает dirty checking
    }

    @Test
    void record_futureDate_throwsBadRequest() {
        LocalDate tomorrow = TODAY.plusDays(1);

        assertThatThrownBy(() -> service.record(userId, tomorrow, new RecordWeightRequest(new BigDecimal("80"))))
                .isInstanceOf(BadRequestException.class);

        verify(weightRepository, never()).save(any());
    }
}
