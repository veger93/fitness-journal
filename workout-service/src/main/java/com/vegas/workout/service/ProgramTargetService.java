package com.vegas.workout.service;

import com.vegas.workout.dto.ExerciseTargetResponse;
import com.vegas.workout.entity.ProgramSlot;
import com.vegas.workout.entity.Workout;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutSet;
import com.vegas.workout.entity.WorkoutStatus;
import com.vegas.workout.program.Progression;
import com.vegas.workout.repository.ProgramRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Цели подходов для идущей тренировки по дню программы: схема места (подходы × повторы, отдых) и подсказка веса
 * по двойной прогрессии. Тренировка не по программе (или день уже не в активной программе) — целей нет.
 */
@Service
@RequiredArgsConstructor
public class ProgramTargetService {

    private final ProgramRepository programRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;

    @Transactional(readOnly = true)
    public Map<UUID, ExerciseTargetResponse> targets(Workout workout) {
        if (workout.getTemplateId() == null) {
            return Map.of();
        }
        Map<UUID, ProgramSlot> slots = new HashMap<>();
        programRepository.findActiveSlots(workout.getUserId(), workout.getTemplateId())
                .forEach(slot -> slots.put(slot.getExercise().getId(), slot));
        if (slots.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ExerciseTargetResponse> result = new HashMap<>();
        for (WorkoutExercise we : workout.getExercises()) {
            UUID exerciseId = we.getExercise().getId();
            ProgramSlot slot = slots.get(exerciseId);
            if (slot == null) {
                continue; // упражнение добавили в тренировку сверх программы — цели нет
            }
            // две последние завершённые тренировки с этим упражнением — правилу «два раза подряд» больше не нужно
            List<List<WorkoutSet>> recent = workoutExerciseRepository
                    .findRecent(workout.getUserId(), exerciseId, WorkoutStatus.COMPLETED, Limit.of(2)).stream()
                    .map(WorkoutExercise::getSets)
                    .toList();
            var scheme = slot.getScheme();
            Progression.Hint hint = Progression.suggest(recent, scheme.getRepsMax(), we.getExercise().getEquipment());
            result.put(exerciseId, new ExerciseTargetResponse(scheme.getSets(), scheme.getRepsMin(),
                    scheme.getRepsMax(), scheme.getRestSec(), hint.kind().name(), hint.suggestedWeightKg(),
                    hint.baseWeightKg(), hint.lastReps()));
        }
        return result;
    }
}
