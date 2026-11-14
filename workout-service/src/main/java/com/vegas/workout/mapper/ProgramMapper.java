package com.vegas.workout.mapper;

import com.vegas.workout.dto.ProgramResponse;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.Program;
import com.vegas.workout.entity.ProgramSlot;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.program.ProgramGenerator;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Программа → ответ API. Руками, а не MapStruct (как WorkoutTemplateMapper): тут не 1:1, а группировка —
 * места программы раскладываем по дням (комплексам) и узнаём, в какие дни недели они стоят в плане.
 */
@Component
public class ProgramMapper {

    /**
     * @param weekDaysByTemplate по текущему плану недели: id комплекса → дни недели (человек мог их переставить).
     *                           Ключ — id, а не сама сущность: после clear() контекста Hibernate (так делает
     *                           WeeklyPlanService.replace) один и тот же комплекс — уже другой Java-объект
     */
    public ProgramResponse toResponse(Program program, Map<UUID, List<DayOfWeek>> weekDaysByTemplate,
                                      LocalDate today) {
        // LinkedHashMap — сохраняем порядок, в котором дни появились; ниже отсортируем по первому дню недели
        Map<WorkoutTemplate, List<ProgramSlot>> byDay = new LinkedHashMap<>();
        program.getSlots().forEach(slot -> byDay.computeIfAbsent(slot.getTemplate(), t -> new ArrayList<>()).add(slot));

        List<ProgramResponse.Day> days = byDay.entrySet().stream()
                .map(entry -> {
                    List<DayOfWeek> weekDays = weekDaysByTemplate.getOrDefault(entry.getKey().getId(), List.of());
                    return new ProgramResponse.Day(entry.getKey().getId(), entry.getKey().getName(), weekDays,
                            entry.getValue().stream()
                                    .sorted(Comparator.comparingInt(ProgramSlot::getPosition))
                                    .map(ProgramMapper::toSlot)
                                    .toList());
                })
                // день, которого нет в плане (убрали руками), — в конец
                .sorted(Comparator.comparing((ProgramResponse.Day d) ->
                        d.weekDays().isEmpty() ? 8 : d.weekDays().get(0).getValue()))
                .toList();

        return new ProgramResponse(program.getId(), ProgramGenerator.splitTitle(program.getDaysPerWeek()),
                program.getGoal(), program.getDaysPerWeek(), program.getSessionMinutes(), program.getLocation(),
                program.getExperienceLevel(), program.getLimits(), program.getPriority(), program.getStartedAt(),
                program.getReviewAt(), program.isReviewDue(today), program.notesList(), days);
    }

    private static ProgramResponse.Slot toSlot(ProgramSlot slot) {
        Exercise exercise = slot.getExercise();
        Exercise original = slot.getOriginalExercise();
        return new ProgramResponse.Slot(slot.getId(), slot.getPosition(), exercise.getId(), exercise.getName(),
                exercise.getEquipment(), exercise.getImageKey(), slot.getMovementPattern(),
                slot.getScheme().getSets(), slot.getScheme().getRepsMin(), slot.getScheme().getRepsMax(),
                slot.getScheme().getRestSec(), slot.isSwapped(), original == null ? null : original.getName());
    }
}
