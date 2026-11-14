package com.vegas.workout.entity;

import com.vegas.common.exception.BadRequestException;
import com.vegas.common.exception.BusinessException;
import com.vegas.common.model.BodyPart;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Правила программы — в самих сущностях, проверяем их без Spring и без базы (как WorkoutTest).
 */
class ProgramTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-11-13");

    private final Exercise barbellBench = exercise("Жим штанги лёжа", "HORIZONTAL_PUSH");
    private final Exercise dumbbellBench = exercise("Жим гантелей лёжа", "HORIZONTAL_PUSH");
    private final Exercise machineBench = exercise("Жим в рычажном тренажёре", "HORIZONTAL_PUSH");
    private final Exercise latPulldown = exercise("Тяга верхнего блока", "VERTICAL_PULL");

    // ---------- анкета ----------

    @Test
    void answers_outOfRange_areRejected() {
        assertThatThrownBy(() -> answers(6, 60)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> answers(3, 50)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> new ProgramAnswers(null, 3, 60, ProgramLocation.GYM, ExperienceLevel.BEGINNER, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void answers_limitsAreCopied_andNullMeansNone() {
        Set<BodyLimit> limits = EnumSet.of(BodyLimit.KNEES);
        ProgramAnswers answers = new ProgramAnswers(ProgramGoal.MUSCLE, 3, 60, ProgramLocation.GYM,
                ExperienceLevel.BEGINNER, limits, null);
        limits.add(BodyLimit.SHOULDERS); // снаружи поменяли — ответы не изменились

        assertThat(answers.limits()).containsExactly(BodyLimit.KNEES);
        assertThat(answers(3, 60).limits()).isEmpty();
    }

    // ---------- программа ----------

    @Test
    void start_isActive_andReviewIn6Weeks() {
        Program program = Program.start(UUID.randomUUID(), answers(3, 60), List.of(), NOW, TODAY);

        assertThat(program.isActive()).isTrue();
        assertThat(program.getReviewAt()).isEqualTo(LocalDate.parse("2026-12-25"));
        assertThat(program.isReviewDue(LocalDate.parse("2026-12-24"))).isFalse();
        assertThat(program.isReviewDue(LocalDate.parse("2026-12-25"))).isTrue();
    }

    @Test
    void answers_roundTrip() {
        ProgramAnswers answers = new ProgramAnswers(ProgramGoal.STRENGTH, 4, 75, ProgramLocation.HOME_DUMBBELLS,
                ExperienceLevel.ADVANCED, Set.of(BodyLimit.LOWER_BACK), PriorityGroup.BACK);

        assertThat(Program.start(UUID.randomUUID(), answers, List.of(), NOW, TODAY).answers()).isEqualTo(answers);
    }

    @Test
    void notes_storedAsLines() {
        Program program = Program.start(UUID.randomUUID(), answers(3, 60), List.of("Первое", "Второе"), NOW, TODAY);

        assertThat(program.notesList()).containsExactly("Первое", "Второе");
        assertThat(Program.start(UUID.randomUUID(), answers(3, 60), List.of(), NOW, TODAY).notesList()).isEmpty();
    }

    @Test
    void archived_isNotActive_andNeverDueForReview() {
        Program program = Program.start(UUID.randomUUID(), answers(3, 60), List.of(), NOW, TODAY);
        program.archive();

        assertThat(program.isActive()).isFalse();
        assertThat(program.isReviewDue(LocalDate.parse("2027-06-01"))).isFalse();
    }

    // ---------- замена упражнения (П14) ----------

    @Test
    void swap_withinFamily_keepsOriginal() {
        ProgramSlot slot = slot(barbellBench);

        slot.swapTo(dumbbellBench);

        assertThat(slot.getExercise()).isSameAs(dumbbellBench);
        assertThat(slot.getOriginalExercise()).isSameAs(barbellBench);
        assertThat(slot.isSwapped()).isTrue();
        assertThat(slot.getMovementPattern()).isEqualTo("HORIZONTAL_PUSH"); // семейство места не меняется
    }

    @Test
    void swap_onlyOnce() {
        ProgramSlot slot = slot(barbellBench);
        slot.swapTo(dumbbellBench);

        assertThatThrownBy(() -> slot.swapTo(machineBench))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("один раз");
    }

    @Test
    void swap_toOtherFamily_isRejected() {
        ProgramSlot slot = slot(barbellBench);

        assertThatThrownBy(() -> slot.swapTo(latPulldown))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("той же работой мышц");
        assertThat(slot.isSwapped()).isFalse(); // неудачная попытка не тратит единственную замену
    }

    @Test
    void swap_toSameOrArchived_isRejected() {
        ProgramSlot slot = slot(barbellBench);
        machineBench.archive(NOW);

        assertThatThrownBy(() -> slot.swapTo(barbellBench)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> slot.swapTo(machineBench)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void slot_withoutFamily_isRejected() {
        Exercise noFamily = exercise("Бег", null);

        assertThatThrownBy(() -> slot(noFamily)).isInstanceOf(BadRequestException.class);
    }

    // ---------- схема подходов ----------

    @Test
    void scheme_bounds() {
        assertThat(SetScheme.of(3, 8, 12, 120).getRepsMax()).isEqualTo((short) 12);
        assertThatThrownBy(() -> SetScheme.of(0, 8, 12, 120)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SetScheme.of(3, 12, 8, 120)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SetScheme.of(3, 8, 12, 10)).isInstanceOf(BadRequestException.class);
    }

    // ---------- помощники ----------

    private ProgramSlot slot(Exercise exercise) {
        Program program = Program.start(UUID.randomUUID(), answers(3, 60), List.of(), NOW, TODAY);
        WorkoutTemplate day = WorkoutTemplate.custom(program.getOwnerId(), "День A · всё тело", "violet", null,
                List.of(exercise));
        return program.addSlot(day, 0, exercise, SetScheme.of(3, 8, 12, 120));
    }

    private static ProgramAnswers answers(int days, int minutes) {
        return new ProgramAnswers(ProgramGoal.MUSCLE, days, minutes, ProgramLocation.GYM,
                ExperienceLevel.BEGINNER, null, null);
    }

    private static Exercise exercise(String name, String pattern) {
        Exercise exercise = Exercise.custom(UUID.randomUUID(), name, BodyPart.CHEST, Equipment.BARBELL,
                TrackingType.WEIGHT_REPS, null, null);
        ReflectionTestUtils.setField(exercise, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(exercise, "movementPattern", pattern);
        return exercise;
    }
}
