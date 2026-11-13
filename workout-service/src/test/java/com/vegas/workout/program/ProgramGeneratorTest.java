package com.vegas.workout.program;

import com.vegas.common.model.BodyPart;
import com.vegas.workout.entity.BodyLimit;
import com.vegas.workout.entity.Equipment;
import com.vegas.workout.entity.Exercise;
import com.vegas.workout.entity.ExperienceLevel;
import com.vegas.workout.entity.PriorityGroup;
import com.vegas.workout.entity.ProgramAnswers;
import com.vegas.workout.entity.ProgramGoal;
import com.vegas.workout.entity.ProgramLocation;
import com.vegas.workout.entity.TrackingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Правила генератора П1–П14 (docs/program.md) — по тесту (или группе тестов) на правило.
 * Каталог — маленькая копия настоящего (те же названия, снаряды и семейства), собранная прямо в тесте.
 */
class ProgramGeneratorTest {

    private final ProgramGenerator generator = new ProgramGenerator();
    private final List<Exercise> catalog = catalog();

    // ---------- П1: сплит ----------

    @Test
    void days2_fullBodyTwice_mondayThursday() {
        GeneratedProgram program = generate(answers(2, 60));

        assertThat(program.title()).isEqualTo("Всё тело × 2");
        assertThat(program.days()).extracting(GeneratedProgram.Day::name)
                .containsExactly("День A · всё тело", "День B · всё тело");
        assertThat(program.schedule()).containsExactly(Map.entry(DayOfWeek.MONDAY, 0), Map.entry(DayOfWeek.THURSDAY, 1));
    }

    @Test
    void days3_alternateABA() {
        GeneratedProgram program = generate(answers(3, 60));

        assertThat(program.days()).hasSize(2); // два разных комплекса
        assertThat(program.schedule()).containsExactly(
                Map.entry(DayOfWeek.MONDAY, 0), Map.entry(DayOfWeek.WEDNESDAY, 1), Map.entry(DayOfWeek.FRIDAY, 0));
    }

    @Test
    void days4_upperLower_days5_pushPullLegsUpperLower() {
        assertThat(generate(answers(4, 60)).days()).extracting(GeneratedProgram.Day::name)
                .containsExactly("Верх A", "Низ A", "Верх B", "Низ B");
        assertThat(generate(answers(5, 60)).days()).extracting(GeneratedProgram.Day::name)
                .containsExactly("Толкай · грудь, плечи, трицепс", "Тяни · спина, бицепс", "Ноги", "Верх A", "Низ B");
    }

    /** П1: грудь, спина и ноги — не реже 2 раз в неделю при любом числе дней и самой короткой тренировке */
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    void majorGroups_atLeastTwicePerWeek(int days) {
        GeneratedProgram program = generate(answers(days, 45));

        Map<PriorityGroup, Integer> sessions = sessionsPerGroup(program);
        assertThat(sessions.get(PriorityGroup.CHEST)).isGreaterThanOrEqualTo(2);
        assertThat(sessions.get(PriorityGroup.BACK)).isGreaterThanOrEqualTo(2);
        assertThat(sessions.get(PriorityGroup.LEGS)).isGreaterThanOrEqualTo(2);
    }

    // ---------- П2, П3: объём ----------

    /** П2: чем опытнее, тем больше подходов на группу в неделю; у новичка — около 10 (ACSM 2026: от ~10) */
    @Test
    void weeklyVolume_growsWithExperience() {
        int beginner = weeklySets(generate(answers(3, 60, ExperienceLevel.BEGINNER)), PriorityGroup.CHEST);
        int intermediate = weeklySets(generate(answers(3, 60, ExperienceLevel.INTERMEDIATE)), PriorityGroup.CHEST);
        int advanced = weeklySets(generate(answers(3, 60, ExperienceLevel.ADVANCED)), PriorityGroup.CHEST);

        assertThat(beginner).isBetween(8, 10);
        assertThat(intermediate).isGreaterThanOrEqualTo(beginner);
        assertThat(advanced).isGreaterThan(intermediate);
    }

    @Test
    void priorityGroup_getsOneMoreSet() {
        ProgramAnswers chest = new ProgramAnswers(ProgramGoal.MUSCLE, 3, 60, ProgramLocation.GYM,
                ExperienceLevel.BEGINNER, null, PriorityGroup.CHEST);

        GeneratedProgram.Slot bench = slot(generate(chest), "HORIZONTAL_PUSH");
        GeneratedProgram.Slot pull = slot(generate(chest), "VERTICAL_PULL");
        assertThat(bench.scheme().getSets()).isEqualTo((short) 4);
        assertThat(pull.scheme().getSets()).isEqualTo((short) 3);
    }

    // ---------- П4, П6: повторы и отдых ----------

    @Test
    void reps_dependOnGoal() {
        assertThat(slot(generate(answers(3, 60)), "SQUAT").scheme().getRepsMax()).isEqualTo((short) 12);
        GeneratedProgram strength = generate(new ProgramAnswers(ProgramGoal.STRENGTH, 3, 60, ProgramLocation.GYM,
                ExperienceLevel.INTERMEDIATE, null, null));
        assertThat(slot(strength, "SQUAT").scheme().getRepsMin()).isEqualTo((short) 4);
        assertThat(slot(strength, "SQUAT").scheme().getRestSec()).isEqualTo((short) 180);
        assertThat(slot(strength, "BICEPS_CURL").scheme().getRepsMin()).isEqualTo((short) 8);
    }

    @Test
    void fatLoss_10to15_andCardioAdvice() {
        GeneratedProgram program = generate(new ProgramAnswers(ProgramGoal.FAT_LOSS, 3, 60, ProgramLocation.GYM,
                ExperienceLevel.BEGINNER, null, null));

        assertThat(slot(program, "SQUAT").scheme().getRepsMin()).isEqualTo((short) 10);
        assertThat(program.notes()).anyMatch(note -> note.contains("кардио"));
    }

    // ---------- П7, П9: порядок и число упражнений ----------

    @ParameterizedTest
    @ValueSource(ints = {45, 60, 75})
    void placesPerDay_dependOnTime(int minutes) {
        GeneratedProgram program = generate(answers(3, minutes));

        int expected = ProgramGenerator.PLACES_BY_MINUTES.get(minutes);
        assertThat(program.days()).allSatisfy(day -> assertThat(day.slots()).hasSize(expected));
    }

    @Test
    void compoundsFirst_inEveryDay() {
        for (int days = 2; days <= 5; days++) {
            for (GeneratedProgram.Day day : generate(answers(days, 75)).days()) {
                List<Boolean> compound = day.slots().stream().map(GeneratedProgram.Slot::compound).toList();
                int firstIsolation = compound.indexOf(false);
                if (firstIsolation >= 0) {
                    // после первой изоляции многосуставных быть не должно (П7)
                    assertThat(compound.subList(firstIsolation, compound.size())).as(day.name()).doesNotContain(true);
                }
            }
        }
    }

    // ---------- П8: выбор упражнения ----------

    @Test
    void sameFamily_otherExercise_onSecondDay() {
        GeneratedProgram program = generate(answers(3, 60, ExperienceLevel.INTERMEDIATE));

        Exercise pressA = slot(program.days().get(0), "HORIZONTAL_PUSH").exercise();
        Exercise pressB = slot(program.days().get(1), "HORIZONTAL_PUSH").exercise();
        assertThat(pressA.getName()).isEqualTo("Жим лёжа");          // классика при свободных весах
        assertThat(pressB.getName()).isNotEqualTo(pressA.getName()); // во второй день — другое (П8)
    }

    @Test
    void noExerciseTwiceInOneDay() {
        for (GeneratedProgram.Day day : generate(answers(5, 75)).days()) {
            assertThat(day.slots()).extracting(s -> s.exercise().getId()).doesNotHaveDuplicates();
        }
    }

    @Test
    void beginnerInGym_startsWithMachines() {
        GeneratedProgram program = generate(answers(3, 60, ExperienceLevel.BEGINNER));

        assertThat(slot(program, "HORIZONTAL_PUSH").exercise().getEquipment()).isEqualTo(Equipment.MACHINE);
    }

    @Test
    void familiarExercise_isPreferred() {
        Exercise dumbbellBench = byName("Жим гантелей лёжа");
        GeneratedProgram program = generator.generate(answers(3, 60, ExperienceLevel.INTERMEDIATE), catalog,
                Set.of(), Set.of(dumbbellBench.getId()));

        assertThat(slot(program, "HORIZONTAL_PUSH").exercise()).isSameAs(dumbbellBench);
    }

    @Test
    void excluded_isNeverChosen() {
        Exercise bench = byName("Жим лёжа");
        GeneratedProgram program = generator.generate(answers(5, 75, ExperienceLevel.ADVANCED), catalog,
                Set.of(bench.getId()), Set.of());

        assertThat(allExercises(program)).doesNotContain(bench);
    }

    @Test
    void customArchivedAssistedTimed_areNeverChosen() {
        assertThat(allExercises(generate(answers(5, 75))))
                .noneMatch(Exercise::isCustom)
                .noneMatch(Exercise::isArchived)
                .noneMatch(Exercise::isAssisted)
                .allMatch(e -> e.getTrackingType() == TrackingType.WEIGHT_REPS);
    }

    // ---------- место и ограничения ----------

    @Test
    void home_onlyDumbbellsAndBodyweight_withHonestNote() {
        ProgramAnswers home = new ProgramAnswers(ProgramGoal.MUSCLE, 3, 60, ProgramLocation.HOME_DUMBBELLS,
                ExperienceLevel.BEGINNER, null, null);
        GeneratedProgram program = generate(home);

        assertThat(allExercises(program)).allMatch(e ->
                e.getEquipment() == Equipment.DUMBBELL || e.getEquipment() == Equipment.BODYWEIGHT);
        assertThat(program.notes()).anyMatch(note -> note.contains("сгибание ног") && note.contains("дома"));
    }

    @Test
    void home_noPullUpsDipsOrHyperextension() {
        ProgramAnswers home = new ProgramAnswers(ProgramGoal.MUSCLE, 5, 75, ProgramLocation.HOME_DUMBBELLS,
                ExperienceLevel.ADVANCED, null, null);

        assertThat(allExercises(generate(home))).extracting(Exercise::getName)
                .doesNotContain("Подтягивания", "Отжимания на брусьях", "Гиперэкстензия");
        assertThat(generate(home).notes()).anyMatch(note -> note.contains("тяга сверху"));
    }

    /** На 4 днях руки тоже работают отдельно: в «Верх A» бицепс, в «Верх B» трицепс даже при часовой тренировке */
    @Test
    void upperLower_hasArmsAt60Minutes() {
        GeneratedProgram program = generate(answers(4, 60));

        assertThat(slot(program.days().get(0), "BICEPS_CURL")).isNotNull();
        assertThat(slot(program.days().get(2), "TRICEPS_EXTENSION")).isNotNull();
    }

    @Test
    void lowerBack_noBarbellHingeOrSquat() {
        ProgramAnswers back = new ProgramAnswers(ProgramGoal.MUSCLE, 4, 75, ProgramLocation.GYM,
                ExperienceLevel.ADVANCED, Set.of(BodyLimit.LOWER_BACK), null);

        assertThat(allExercises(generate(back))).noneMatch(e -> e.getEquipment() == Equipment.BARBELL
                && Set.of("HINGE", "SQUAT", "HORIZONTAL_PULL", "VERTICAL_PUSH").contains(e.getMovementPattern()));
    }

    @Test
    void knees_noLunges_noBarbellSquat_withNote() {
        ProgramAnswers knees = new ProgramAnswers(ProgramGoal.MUSCLE, 4, 75, ProgramLocation.GYM,
                ExperienceLevel.ADVANCED, Set.of(BodyLimit.KNEES), null);
        GeneratedProgram program = generate(knees);

        assertThat(allExercises(program)).noneMatch(e -> "LUNGE".equals(e.getMovementPattern()));
        assertThat(allExercises(program)).noneMatch(e -> "SQUAT".equals(e.getMovementPattern())
                && e.getEquipment() == Equipment.BARBELL);
    }

    @Test
    void shoulders_noDipsNoUprightRow() {
        Exercise dips = byName("Отжимания на брусьях");
        Exercise uprightRow = byName("Тяга штанги к подбородку");

        assertThat(ProgramGenerator.blockedByLimits(dips, Set.of(BodyLimit.SHOULDERS))).isTrue();
        assertThat(ProgramGenerator.blockedByLimits(uprightRow, Set.of(BodyLimit.SHOULDERS))).isTrue();
        assertThat(ProgramGenerator.blockedByLimits(dips, Set.of())).isFalse();
    }

    // ---------- детерминизм ----------

    @Test
    void sameInput_sameProgram() {
        assertThat(generate(answers(4, 60))).isEqualTo(generate(answers(4, 60)));
    }

    // ---------- помощники ----------

    private GeneratedProgram generate(ProgramAnswers answers) {
        return generator.generate(answers, catalog, Set.of(), Set.of());
    }

    private static ProgramAnswers answers(int days, int minutes) {
        return answers(days, minutes, ExperienceLevel.BEGINNER);
    }

    private static ProgramAnswers answers(int days, int minutes, ExperienceLevel level) {
        return new ProgramAnswers(ProgramGoal.MUSCLE, days, minutes, ProgramLocation.GYM, level, null, null);
    }

    private static GeneratedProgram.Slot slot(GeneratedProgram program, String family) {
        return program.days().stream().flatMap(d -> d.slots().stream())
                .filter(s -> family.equals(s.movementPattern())).findFirst().orElseThrow();
    }

    private static GeneratedProgram.Slot slot(GeneratedProgram.Day day, String family) {
        return day.slots().stream().filter(s -> family.equals(s.movementPattern())).findFirst().orElseThrow();
    }

    private static List<Exercise> allExercises(GeneratedProgram program) {
        return program.days().stream().flatMap(d -> d.slots().stream()).map(GeneratedProgram.Slot::exercise).toList();
    }

    /** Сколько тренировок в неделю работает группа (по расписанию: день A в 3-дневке считается дважды) */
    private static Map<PriorityGroup, Integer> sessionsPerGroup(GeneratedProgram program) {
        Map<PriorityGroup, Integer> sessions = new HashMap<>();
        program.schedule().values().forEach(dayIndex -> program.days().get(dayIndex).slots().stream()
                .map(s -> Families.group(s.movementPattern()).orElse(null))
                .filter(g -> g != null)
                .distinct()
                .forEach(g -> sessions.merge(g, 1, Integer::sum)));
        return sessions;
    }

    private static int weeklySets(GeneratedProgram program, PriorityGroup group) {
        return program.schedule().values().stream()
                .flatMap(dayIndex -> program.days().get(dayIndex).slots().stream())
                .filter(s -> Families.group(s.movementPattern()).filter(group::equals).isPresent())
                .mapToInt(s -> s.scheme().getSets())
                .sum();
    }

    private Exercise byName(String name) {
        return catalog.stream().filter(e -> e.getName().equals(name)).findFirst().orElseThrow();
    }

    /** Копия части настоящего каталога: названия, снаряды, семейства — как в базе (миграции 007–020) */
    private static List<Exercise> catalog() {
        List<Exercise> list = new ArrayList<>();
        add(list, "Приседания со штангой", Equipment.BARBELL, "SQUAT");
        add(list, "Фронтальный присед", Equipment.BARBELL, "SQUAT");
        add(list, "Жим ногами", Equipment.MACHINE, "SQUAT");
        add(list, "Гакк-присед в тренажёре", Equipment.MACHINE, "SQUAT");
        add(list, "Приседания с гантелями", Equipment.DUMBBELL, "SQUAT");
        add(list, "Румынская тяга", Equipment.BARBELL, "HINGE");
        add(list, "Становая тяга", Equipment.BARBELL, "HINGE");
        add(list, "Становая тяга с гантелями", Equipment.DUMBBELL, "HINGE");
        add(list, "Гиперэкстензия", Equipment.BODYWEIGHT, "HINGE");
        add(list, "Ягодичный мост", Equipment.BODYWEIGHT, "HINGE");
        add(list, "Выпады с гантелями", Equipment.DUMBBELL, "LUNGE");
        add(list, "Обратные выпады с гантелями", Equipment.DUMBBELL, "LUNGE");
        add(list, "Жим лёжа", Equipment.BARBELL, "HORIZONTAL_PUSH");
        add(list, "Жим лёжа на наклонной скамье", Equipment.BARBELL, "HORIZONTAL_PUSH");
        add(list, "Жим гантелей лёжа", Equipment.DUMBBELL, "HORIZONTAL_PUSH");
        add(list, "Жим гантелей на наклонной скамье", Equipment.DUMBBELL, "HORIZONTAL_PUSH");
        add(list, "Жим от груди в тренажёре", Equipment.MACHINE, "HORIZONTAL_PUSH");
        add(list, "Отжимания от пола", Equipment.BODYWEIGHT, "HORIZONTAL_PUSH");
        add(list, "Отжимания на брусьях", Equipment.BODYWEIGHT, "HORIZONTAL_PUSH");
        add(list, "Жим штанги стоя", Equipment.BARBELL, "VERTICAL_PUSH");
        add(list, "Жим гантелей сидя", Equipment.DUMBBELL, "VERTICAL_PUSH");
        add(list, "Жим Арнольда", Equipment.DUMBBELL, "VERTICAL_PUSH");
        add(list, "Тяга верхнего блока", Equipment.MACHINE, "VERTICAL_PULL");
        add(list, "Вертикальная тяга в рычажном тренажёре", Equipment.MACHINE, "VERTICAL_PULL");
        add(list, "Подтягивания", Equipment.BODYWEIGHT, "VERTICAL_PULL");
        add(list, "Тяга штанги в наклоне", Equipment.BARBELL, "HORIZONTAL_PULL");
        add(list, "Тяга горизонтального блока", Equipment.MACHINE, "HORIZONTAL_PULL");
        add(list, "Тяга гантели одной рукой", Equipment.DUMBBELL, "HORIZONTAL_PULL");
        add(list, "Разводка гантелей", Equipment.DUMBBELL, "CHEST_FLY");
        add(list, "Сведение рук в тренажёре", Equipment.MACHINE, "CHEST_FLY");
        add(list, "Махи гантелями в стороны", Equipment.DUMBBELL, "LATERAL_RAISE");
        add(list, "Тяга штанги к подбородку", Equipment.BARBELL, "LATERAL_RAISE");
        add(list, "Обратные разведения в тренажёре", Equipment.MACHINE, "REAR_DELT");
        add(list, "Разведения гантелей в наклоне", Equipment.DUMBBELL, "REAR_DELT");
        add(list, "Подъём штанги на бицепс", Equipment.BARBELL, "BICEPS_CURL");
        add(list, "Подъём гантелей на бицепс", Equipment.DUMBBELL, "BICEPS_CURL");
        add(list, "Молотки", Equipment.DUMBBELL, "BICEPS_CURL");
        add(list, "Разгибание рук на блоке", Equipment.MACHINE, "TRICEPS_EXTENSION");
        add(list, "Французский жим гантели из-за головы", Equipment.DUMBBELL, "TRICEPS_EXTENSION");
        add(list, "Сгибание ног в тренажёре", Equipment.MACHINE, "LEG_CURL");
        add(list, "Подъём на носки в тренажёре", Equipment.MACHINE, "CALF_RAISE");
        add(list, "Скручивания", Equipment.BODYWEIGHT, "ABS");
        // не годятся генератору: без семейства, на время, гравитрон, своё, в архиве
        add(list, "Разгибание ног в тренажёре", Equipment.MACHINE, null);
        Exercise plank = add(list, "Планка", Equipment.BODYWEIGHT, "PLANK");
        ReflectionTestUtils.setField(plank, "trackingType", TrackingType.TIME);
        Exercise gravitron = add(list, "Подтягивания в гравитроне", Equipment.MACHINE, "VERTICAL_PULL");
        ReflectionTestUtils.setField(gravitron, "assisted", true);
        Exercise mine = add(list, "Моя тяга", Equipment.MACHINE, "VERTICAL_PULL");
        ReflectionTestUtils.setField(mine, "ownerId", UUID.randomUUID());
        Exercise old = add(list, "Старая тяга блока", Equipment.MACHINE, "VERTICAL_PULL");
        ReflectionTestUtils.setField(old, "archivedAt", java.time.Instant.parse("2026-01-01T00:00:00Z"));
        return list;
    }

    private static Exercise add(List<Exercise> list, String name, Equipment equipment, String family) {
        Exercise exercise = Exercise.custom(UUID.randomUUID(), name, BodyPart.CHEST, equipment,
                TrackingType.WEIGHT_REPS, null, null);
        ReflectionTestUtils.setField(exercise, "id", UUID.nameUUIDFromBytes(name.getBytes()));
        ReflectionTestUtils.setField(exercise, "ownerId", null); // системное
        ReflectionTestUtils.setField(exercise, "movementPattern", family);
        list.add(exercise);
        return exercise;
    }
}
