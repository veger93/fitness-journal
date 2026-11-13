package com.vegas.workout.entity;

import com.vegas.common.exception.NotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Программа тренировок, подобранная по анкете (docs/program.md).
 * Агрегат: места в программе (ProgramSlot) живут и сохраняются только вместе с ней (cascade + orphanRemoval).
 * Дни программы — обычные комплексы WorkoutTemplate, на них ссылаются места.
 */
@Entity
@Table(name = "programs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Program {

    /** Через сколько недель предлагаем пересмотреть программу (П11) */
    public static final int REVIEW_AFTER_WEEKS = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProgramStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramGoal goal;

    @Column(name = "days_per_week", nullable = false)
    private short daysPerWeek;

    @Column(name = "session_minutes", nullable = false)
    private short sessionMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramLocation location;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 20)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PriorityGroup priority;

    /**
     * @ElementCollection — набор простых значений в отдельной таблице program_limits (как muscles у Exercise).
     * Не сущность: своего id нет, живёт и удаляется вместе с программой.
     */
    @ElementCollection
    @CollectionTable(name = "program_limits", joinColumns = @JoinColumn(name = "program_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "body_limit", nullable = false, length = 20)
    private Set<BodyLimit> limits = new HashSet<>();

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "review_at", nullable = false)
    private LocalDate reviewAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * mappedBy: внешний ключ program_id хранит ProgramSlot (поле program), здесь — обратная сторона связи.
     * cascade ALL + orphanRemoval: сохранили программу — сохранились места; убрали место из списка — удалилось из БД.
     */
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ProgramSlot> slots = new ArrayList<>();

    /** Новая активная программа. today — по часам пользователя: от него считаем дату пересмотра */
    public static Program start(UUID ownerId, ProgramAnswers answers, Instant now, LocalDate today) {
        Program program = new Program();
        program.ownerId = ownerId;
        program.status = ProgramStatus.ACTIVE;
        program.goal = answers.goal();
        program.daysPerWeek = (short) answers.daysPerWeek();
        program.sessionMinutes = (short) answers.sessionMinutes();
        program.location = answers.location();
        program.experienceLevel = answers.experienceLevel();
        program.priority = answers.priority();
        program.limits.addAll(answers.limits());
        program.startedAt = now;
        program.reviewAt = today.plusWeeks(REVIEW_AFTER_WEEKS);
        return program;
    }

    /** Ответы анкеты обратно одним значением — для пересмотра («пройти заново» с подставленными ответами) */
    public ProgramAnswers answers() {
        return new ProgramAnswers(goal, daysPerWeek, sessionMinutes, location, experienceLevel, limits, priority);
    }

    /** Место в программе: упражнение на позиции position в комплексе дня, со схемой подходов */
    public ProgramSlot addSlot(WorkoutTemplate day, int position, Exercise exercise, SetScheme scheme) {
        ProgramSlot slot = new ProgramSlot(this, day, position, exercise, scheme);
        slots.add(slot);
        return slot;
    }

    public ProgramSlot findSlot(UUID slotId) {
        return slots.stream()
                .filter(slot -> slotId.equals(slot.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Место в программе не найдено"));
    }

    /** Прошёл анкету заново — старая программа в архив (активная у человека одна — индекс в миграции 022) */
    public void archive() {
        status = ProgramStatus.ARCHIVED;
    }

    public boolean isActive() {
        return status == ProgramStatus.ACTIVE;
    }

    /** Пора предложить пересмотр: прошло 6 недель */
    public boolean isReviewDue(LocalDate today) {
        return isActive() && !today.isBefore(reviewAt);
    }
}
