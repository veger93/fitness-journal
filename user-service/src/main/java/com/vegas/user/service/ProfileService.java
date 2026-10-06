package com.vegas.user.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.user.dto.ProfileResponse;
import com.vegas.user.dto.UpdateProfileRequest;
import com.vegas.user.entity.UserProfile;
import com.vegas.user.event.OutboxService;
import com.vegas.user.mapper.ProfileMapper;
import com.vegas.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    static final int MIN_AGE = 10;
    static final int MAX_AGE = 100;

    private final UserProfileRepository profileRepository;
    private final BodyWeightService bodyWeightService;
    private final ProfileMapper profileMapper;
    private final Clock clock;
    private final OutboxService outboxService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        UserProfile profile = findProfile(userId);
        BigDecimal currentWeight = bodyWeightService.currentWeight(userId).orElse(null);
        return profileMapper.toResponse(profile, currentWeight);
    }

    /**
     * Онбординг и редактирование физ. данных.
     * save() не вызываем: profile загружен в этой транзакции -> dirty checking сделает UPDATE сам.
     */
    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        validateBirthYear(request.birthYear());

        UserProfile profile = findProfile(userId);
        profile.setGender(request.gender());
        profile.setBirthYear(request.birthYear().shortValue());
        profile.setHeightCm(request.heightCm().shortValue());
        profile.setExperienceLevel(request.experienceLevel());
        profile.setOnboardingCompleted(true);

        recordWeightIfChanged(userId, request.weightKg());
        // аналитике нужен уровень подготовки (эталонная кривая) — сообщаем об изменении
        outboxService.profileUpdated(userId);

        return profileMapper.toResponse(profile, request.weightKg());
    }

    /**
     * Экран настроек присылает вес при каждом сохранении. Если он не менялся,
     * новую запись не создаём, иначе история заполнится одинаковыми точками.
     *
     * compareTo, а не equals: для BigDecimal 80.0.equals(80.00) == false (разный scale),
     * а compareTo сравнивает только значение.
     */
    private void recordWeightIfChanged(UUID userId, BigDecimal newWeight) {
        boolean changed = bodyWeightService.currentWeight(userId)
                .map(current -> current.compareTo(newWeight) != 0)
                .orElse(true);
        if (changed) {
            // "сегодня" по часам сервера. Для пользователей в других часовых поясах
            // точнее отдельный эндпоинт PUT /weight/{date}, где дату присылает фронт.
            bodyWeightService.upsert(userId, newWeight, LocalDate.now(clock));
        }
    }

    private void validateBirthYear(int birthYear) {
        int age = Year.now(clock).getValue() - birthYear;
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new BadRequestException("Возраст должен быть от " + MIN_AGE + " до " + MAX_AGE + " лет");
        }
    }

    private UserProfile findProfile(UUID userId) {
        // профиль создаётся при регистрации, поэтому "не найден" = пользователя больше нет
        return profileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Профиль не найден"));
    }
}
