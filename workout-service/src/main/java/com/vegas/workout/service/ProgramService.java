package com.vegas.workout.service;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.exception.NotFoundException;
import com.vegas.workout.dto.ExclusionResponse;
import com.vegas.workout.dto.ProgramAlternativeResponse;
import com.vegas.workout.dto.ProgramRequest;
import com.vegas.workout.dto.ProgramResponse;
import com.vegas.workout.dto.ProgramSwapRequest;
import com.vegas.workout.dto.WeeklyPlanRequest;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.ExerciseExclusion;
import com.vegas.workout.entity.Program;
import com.vegas.workout.entity.ProgramAnswers;
import com.vegas.workout.entity.ProgramSlot;
import com.vegas.workout.entity.WeeklyPlanDay;
import com.vegas.workout.entity.WorkoutExercise;
import com.vegas.workout.entity.WorkoutSet;
import com.vegas.workout.entity.WorkoutStatus;
import com.vegas.workout.entity.WorkoutTemplate;
import com.vegas.workout.mapper.ProgramMapper;
import com.vegas.workout.program.GeneratedProgram;
import com.vegas.workout.program.ProgramGenerator;
import com.vegas.workout.repository.ExerciseExclusionRepository;
import com.vegas.workout.repository.ExerciseRepository;
import com.vegas.workout.repository.ProgramRepository;
import com.vegas.workout.repository.WeeklyPlanDayRepository;
import com.vegas.workout.repository.WorkoutExerciseRepository;
import com.vegas.workout.repository.WorkoutTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Программа тренировок (docs/program.md): создать по анкете, показать, заменить упражнение, «больше не предлагать».
 *
 * Сам генератор ничего не знает о базе — сервис собирает для него данные (каталог, исключения, «что уже делал»)
 * и превращает результат в обычные сущности: комплексы дней, программу с местами и план недели.
 */
@Service
@RequiredArgsConstructor
public class ProgramService {

    /** Цвета плиток комплексов — ключи палитры фронта (как у «Моих комплексов»), по очереди для дней программы */
    private static final List<String> DAY_COLORS = List.of("orange", "blue", "green", "violet");

    private final ProgramRepository programRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseExclusionRepository exclusionRepository;
    private final WorkoutExerciseRepository workoutExerciseRepository;
    private final WorkoutTemplateRepository templateRepository;
    private final WeeklyPlanDayRepository planRepository;
    private final WeeklyPlanService weeklyPlanService;
    private final ProgramGenerator generator;
    private final ProgramMapper mapper;
    private final Clock clock;

    /**
     * Новая программа по анкете. В одной транзакции:
     * 1) прежняя активная — в архив (её комплексы остаются в «Моих комплексах»);
     * 2) генератор собирает программу; 3) дни становятся комплексами, места — строками program_slots;
     * 4) план недели заменяется днями программы (решение 13.11: комплексы остаются, план — новый).
     */
    @Transactional
    public ProgramResponse create(UUID userId, ProgramRequest request) {
        ProgramAnswers answers = request.toAnswers();
        programRepository.findActive(userId).ifPresent(old -> {
            old.archive();
            // сразу в базу: иначе INSERT новой активной программы мог бы уйти раньше UPDATE старой
            // и упереться в частичный уникальный индекс «одна активная на человека»
            programRepository.flush();
        });

        GeneratedProgram generated = generator.generate(answers,
                exerciseRepository.findAllByOwnerIdIsNullAndArchivedAtIsNull(),
                exclusionRepository.findExerciseIdsByOwnerId(userId),
                workoutExerciseRepository.findDoneExerciseIds(userId, WorkoutStatus.COMPLETED));

        Program program = Program.start(userId, answers, generated.notes(), clock.instant(), LocalDate.now(clock));
        List<WorkoutTemplate> templates = new ArrayList<>();
        for (int i = 0; i < generated.days().size(); i++) {
            GeneratedProgram.Day day = generated.days().get(i);
            List<Exercise> exercises = day.slots().stream().map(GeneratedProgram.Slot::exercise).toList();
            WorkoutTemplate template = templateRepository.save(WorkoutTemplate.custom(userId,
                    uniqueTemplateName(userId, day.name()), DAY_COLORS.get(i % DAY_COLORS.size()), null, exercises));
            templates.add(template);
            for (int position = 0; position < day.slots().size(); position++) {
                GeneratedProgram.Slot slot = day.slots().get(position);
                program.addSlot(template, position, slot.exercise(), slot.scheme());
            }
        }
        programRepository.save(program);

        weeklyPlanService.replace(userId, new WeeklyPlanRequest(generated.schedule().entrySet().stream()
                .map(entry -> new WeeklyPlanRequest.Day(entry.getKey(), templates.get(entry.getValue()).getId()))
                .toList()));
        return toResponse(userId, program);
    }

    @Transactional(readOnly = true)
    public Optional<ProgramResponse> getCurrent(UUID userId) {
        return programRepository.findActive(userId).map(program -> toResponse(userId, program));
    }

    /** «Отказаться от программы»: в архив. План и комплексы остаются как есть — человек сам решит, что с ними делать */
    @Transactional
    public void abandon(UUID userId) {
        findActive(userId).archive();
    }

    // ---------- замена упражнения (П14) ----------

    /**
     * Варианты для окна «Заменить»: то же семейство, подходит человеку (место, ограничения, «не предлагать»),
     * ещё нет в этом дне. Сначала то, что уже делал, — с последним результатом.
     */
    @Transactional(readOnly = true)
    public List<ProgramAlternativeResponse> alternatives(UUID userId, UUID slotId) {
        Program program = findActive(userId);
        ProgramSlot slot = notSwappedYet(program.findSlot(slotId));
        Set<UUID> done = workoutExerciseRepository.findDoneExerciseIds(userId, WorkoutStatus.COMPLETED);
        return candidates(userId, program, slot).stream()
                .sorted(Comparator.comparing((Exercise e) -> !done.contains(e.getId())).thenComparing(Exercise::getName))
                .map(e -> toAlternative(userId, e, done.contains(e.getId())))
                .toList();
    }

    @Transactional
    public ProgramResponse swap(UUID userId, UUID slotId, ProgramSwapRequest request) {
        Program program = findActive(userId);
        // проверяем раньше подбора вариантов: иначе на повторную замену ответили бы «нельзя поставить» вместо понятного 409
        ProgramSlot slot = notSwappedYet(program.findSlot(slotId));
        Exercise replacement = candidates(userId, program, slot).stream()
                .filter(e -> e.getId().equals(request.exerciseId()))
                .findFirst()
                // нет среди вариантов: другое семейство, не подходит по месту/ограничениям или уже есть в этом дне
                .orElseThrow(() -> new BadRequestException("Это упражнение нельзя поставить на это место"));

        Exercise current = slot.getExercise();
        slot.swapTo(replacement);                                  // правила П14 — в сущности
        slot.getTemplate().replaceExercise(current, replacement);  // и в комплексе дня, на то же место
        if (request.excludeOld() && !exclusionRepository.existsById(new ExerciseExclusion.Key(userId, current.getId()))) {
            exclusionRepository.save(new ExerciseExclusion(userId, current.getId()));
        }
        return toResponse(userId, program);
    }

    // ---------- «больше не предлагать» ----------

    @Transactional(readOnly = true)
    public List<ExclusionResponse> exclusions(UUID userId) {
        Set<UUID> ids = exclusionRepository.findExerciseIdsByOwnerId(userId);
        return exerciseRepository.findAllById(ids).stream()
                .map(e -> new ExclusionResponse(e.getId(), e.getName()))
                .sorted(Comparator.comparing(ExclusionResponse::exerciseName))
                .toList();
    }

    /** «Вернуть»: упражнение снова может попасть в программу и в варианты замены */
    @Transactional
    public void removeExclusion(UUID userId, UUID exerciseId) {
        exclusionRepository.deleteById(new ExerciseExclusion.Key(userId, exerciseId));
    }

    // ---------- вспомогательное ----------

    private Program findActive(UUID userId) {
        return programRepository.findActive(userId)
                .orElseThrow(() -> new NotFoundException("Программы пока нет — пройдите анкету"));
    }

    /** П14: одна замена на место (то же правило есть в ProgramSlot.swapTo — здесь ради понятного ответа раньше) */
    private static ProgramSlot notSwappedYet(ProgramSlot slot) {
        if (slot.isSwapped()) {
            throw new BusinessException("Это упражнение уже заменяли — заменить можно один раз");
        }
        return slot;
    }

    private List<Exercise> candidates(UUID userId, Program program, ProgramSlot slot) {
        ProgramAnswers answers = program.answers();
        Set<UUID> excluded = exclusionRepository.findExerciseIdsByOwnerId(userId);
        List<Exercise> inDay = slot.getTemplate().getExercises();
        return exerciseRepository.findAllByOwnerIdIsNullAndArchivedAtIsNull().stream()
                .filter(e -> slot.getMovementPattern().equals(e.getMovementPattern()))
                .filter(e -> ProgramGenerator.usable(e, answers, excluded))
                .filter(e -> !inDay.contains(e))
                .toList();
    }

    /** Последний результат: лучший рабочий подход последней тренировки с этим упражнением */
    private ProgramAlternativeResponse toAlternative(UUID userId, Exercise exercise, boolean familiar) {
        Optional<WorkoutExercise> last = familiar
                ? workoutExerciseRepository.findRecent(userId, exercise.getId(), WorkoutStatus.COMPLETED, Limit.of(1))
                        .stream().findFirst()
                : Optional.empty();
        Optional<WorkoutSet> best = last.flatMap(we -> we.getSets().stream()
                // рабочие подходы; «лучший» — тяжелее, при равном весе — больше повторов.
                // Без веса (подтягивания) тоже считаем: тогда покажем хотя бы повторы
                .filter(s -> !s.isWarmup() && s.getReps() != null)
                .max(Comparator.comparing(WorkoutSet::getWeightKg, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(WorkoutSet::getReps)));
        return new ProgramAlternativeResponse(exercise.getId(), exercise.getName(), exercise.getEquipment(),
                exercise.getImageKey(), familiar,
                best.map(WorkoutSet::getWeightKg).orElse(null),
                best.map(WorkoutSet::getReps).orElse(null),
                last.map(we -> we.getWorkout().getCompletedAt()).orElse(null));
    }

    /** «День A · всё тело» уже есть (прошлая программа) — «День A · всё тело (2)»: имена комплексов уникальны */
    private String uniqueTemplateName(UUID userId, String name) {
        String candidate = name;
        for (int n = 2; templateRepository.existsByOwnerIdAndNameIgnoreCase(userId, candidate); n++) {
            candidate = name + " (" + n + ")";
        }
        return candidate;
    }

    private ProgramResponse toResponse(UUID userId, Program program) {
        Map<UUID, List<DayOfWeek>> weekDays = new HashMap<>();
        for (WeeklyPlanDay day : planRepository.findByUserId(userId)) {
            weekDays.computeIfAbsent(day.getTemplate().getId(), id -> new ArrayList<>()).add(day.getDayOfWeek());
        }
        weekDays.values().forEach(days -> days.sort(Comparator.naturalOrder()));
        return mapper.toResponse(program, weekDays, LocalDate.now(clock));
    }
}
