package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.workout.dto.WeeklyPlanRequest;
import com.vegas.workout.dto.WeeklyPlanResponse;
import com.vegas.common.event.WeeklyPlanUpdatedEvent;
import com.vegas.workout.entity.WeeklyPlanDay;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.event.OutboxService;
import com.vegas.workout.mapper.WorkoutTemplateMapper;
import com.vegas.workout.repository.WeeklyPlanDayRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
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
    private final OutboxService outboxService;
    private final Clock clock;

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
        List<WeeklyPlanDay> saved = planRepository.saveAll(days);
        // в той же транзакции, что и план: сохранился план — гарантированно уйдёт и событие (transactional outbox)
        publish(userId, saved);
        return toResponse(saved);
    }

    /**
     * Комплекс переименовали — если он стоит в плане, в напоминаниях должно быть новое название.
     * Вызывается из WorkoutTemplateService внутри его транзакции.
     */
    @Transactional
    public void templateRenamed(UUID userId, UUID templateId) {
        List<WeeklyPlanDay> plan = planRepository.findByUserId(userId);
        if (usesTemplate(plan, templateId)) {
            publish(userId, plan);
        }
    }

    /**
     * Комплекс удаляют — его дни уйдут из плана (в БД ON DELETE CASCADE).
     * Вызывается ДО удаления: отправляем план уже без этого комплекса.
     */
    @Transactional
    public void templateDeleted(UUID userId, UUID templateId) {
        List<WeeklyPlanDay> plan = planRepository.findByUserId(userId);
        if (usesTemplate(plan, templateId)) {
            publish(userId, plan.stream().filter(day -> !day.getTemplate().getId().equals(templateId)).toList());
        }
    }

    private static boolean usesTemplate(List<WeeklyPlanDay> plan, UUID templateId) {
        return plan.stream().anyMatch(day -> day.getTemplate().getId().equals(templateId));
    }

    private void publish(UUID userId, List<WeeklyPlanDay> plan) {
        List<WeeklyPlanUpdatedEvent.PlannedDay> days = plan.stream()
                .sorted(Comparator.comparing(WeeklyPlanDay::getDayOfWeek))
                .map(day -> new WeeklyPlanUpdatedEvent.PlannedDay(day.getDayOfWeek(), day.getTemplate().getName()))
                .toList();
        outboxService.weeklyPlanUpdated(new WeeklyPlanUpdatedEvent(userId, days, clock.instant()));
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
