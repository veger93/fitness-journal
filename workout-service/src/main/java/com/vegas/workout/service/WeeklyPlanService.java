package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.workout.dto.WeeklyPlanRequest;
import com.vegas.workout.dto.WeeklyPlanResponse;
import com.vegas.workout.entity.WeeklyPlanDay;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.mapper.WorkoutTemplateMapper;
import com.vegas.workout.repository.WeeklyPlanDayRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * План на неделю: комплекс на день недели.
 * Почему план — это повторяющаяся неделя, а не "каждый день новое": прогресс видно, когда упражнения
 * повторяются и растут веса; программу обычно меняют раз в несколько недель (ACSM, 2009 — [16]).
 */
@Service
@RequiredArgsConstructor
public class WeeklyPlanService {

    private final WeeklyPlanDayRepository planRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final WorkoutTemplateMapper templateMapper;

    @Transactional(readOnly = true)
    public WeeklyPlanResponse get(UUID userId) {
        return toResponse(planRepository.findByUserId(userId));
    }

    /** PUT: старый план удаляется целиком, новый сохраняется — в одной транзакции. */
    @Transactional
    public WeeklyPlanResponse replace(UUID userId, WeeklyPlanRequest request) {
        Set<DayOfWeek> seen = new HashSet<>();
        for (WeeklyPlanRequest.Day day : request.days()) {
            if (!seen.add(day.dayOfWeek())) {
                throw new BadRequestException("День " + day.dayOfWeek() + " указан дважды");
            }
        }

        planRepository.deleteAllByUserId(userId);
        List<WeeklyPlanDay> days = request.days().stream()
                .filter(day -> day.templateId() != null)
                .map(day -> new WeeklyPlanDay(userId, day.dayOfWeek(), findVisible(userId, day.templateId())))
                .toList();
        return toResponse(planRepository.saveAll(days));
    }

    private WorkoutTemplate findVisible(UUID userId, UUID templateId) {
        return templateRepository.findWithExercisesById(templateId)
                .filter(template -> template.isVisibleTo(userId))
                .orElseThrow(() -> new NotFoundException("Комплекс не найден"));
    }

    /** Всегда 7 дней с понедельника — фронту не нужно достраивать неделю. */
    private WeeklyPlanResponse toResponse(List<WeeklyPlanDay> planned) {
        Map<DayOfWeek, WorkoutTemplate> byDay = new EnumMap<>(DayOfWeek.class);
        planned.forEach(day -> byDay.put(day.getDayOfWeek(), day.getTemplate()));
        return new WeeklyPlanResponse(Arrays.stream(DayOfWeek.values())
                .map(day -> new WeeklyPlanResponse.Day(day,
                        byDay.containsKey(day) ? templateMapper.toResponse(byDay.get(day)) : null))
                .toList());
    }
}
