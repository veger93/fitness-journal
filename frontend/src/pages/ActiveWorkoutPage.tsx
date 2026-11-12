import { useState } from 'react';
import type { SetInput, TrackingType, Workout, WorkoutExercise, WorkoutSet } from '../api/workouts';
import { Button, ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Chip } from '../components/ui/Chip';
import { AddExerciseDialog } from '../components/workout/AddExerciseDialog';
import { ConfirmDialog } from '../components/workout/ConfirmDialog';
import { ExerciseCard } from '../components/workout/ExerciseCard';
import { SaveTemplateDialog } from '../components/workout/SaveTemplateDialog';
import { SetDialog } from '../components/workout/SetDialog';
import { useNow } from '../hooks/useNow';
import {
  newClientId,
  useCurrentWorkout,
  usePendingSets,
  useWorkoutActions,
  type PendingSet,
} from '../hooks/useWorkout';
import { describeSet, formatClock, runParts, toRequest } from '../lib/workout';
import { setsLabel } from '../lib/exercise';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { PageHeader } from '../components/ui/PageHeader';
import { SectionLabel } from '../components/ui/SectionLabel';
import { ListRow, ListRowGroup } from '../components/ui/ListRow';
import styles from './WorkoutPages.module.css';

/** Две колонки — только на широком экране: до 768px «Записать подход» закреплена внизу (data-dock) */
const DESKTOP = '(min-width: 900px)';

export function ActiveWorkoutPage() {
  const current = useCurrentWorkout();
  if (current.isPending) {
    return <p className={styles.muted}>Загружаем тренировку…</p>;
  }
  if (current.isError) {
    return <p className={styles.muted}>{current.error.message}</p>;
  }
  if (!current.data) {
    // Не редирект, а сообщение: после "Завершить" кэш очищается раньше, чем React Router закончит переход
    // на экран итогов, и автоматический <Navigate> здесь перебил бы тот переход.
    return (
      <div className={styles.wide}>
        <PageHeader title="Тренировка" subtitle="Сейчас тренировка не идёт." />
        <ButtonLink to="/workout" className={styles.alignStart}>
          Начать тренировку
        </ButtonLink>
      </div>
    );
  }
  // key: другая тренировка — компонент создаётся заново, состояние (выбранное упражнение) не переносится
  return <ActiveWorkout key={current.data.id} workout={current.data} />;
}

type Modal =
  | { kind: 'set'; editing: WorkoutSet | null }
  | { kind: 'addExercise' }
  | { kind: 'complete' }
  | { kind: 'cancel' }
  | { kind: 'saveTemplate' }
  | null;

function ActiveWorkout({ workout }: { workout: Workout }) {
  const actions = useWorkoutActions(workout);
  const pending = usePendingSets(workout.id);

  const now = useNow();
  const [selectedId, setSelectedId] = useState<string | null>(workout.exercises[0]?.id ?? null);
  const [modal, setModal] = useState<Modal>(null);

  const saved = workout.exercises.find((e) => e.id === selectedId) ?? workout.exercises[0] ?? null;
  // на экране подходы — сохранённые + ещё не подтверждённые сервером (те идут в конце, с пометкой)
  const exercise = saved ? withPendingSets(saved, pending) : null;
  const pendingById = new Map(pending.map((p) => [pendingSetId(p.clientId), p]));
  const saving = pending.filter((p) => p.status === 'saving').length;
  const failed = pending.filter((p) => p.status === 'failed').length;
  const elapsedSec = workout.startedAt ? (now - new Date(workout.startedAt).getTime()) / 1000 : 0;
  const hasSets = workout.exercises.some((e) => e.sets.length > 0) || pending.length > 0;

  const closeModal = () => {
    setModal(null);
    actions.updateSet.reset();
    actions.deleteSet.reset();
  };

  const saveSet = (input: SetInput) => {
    if (!exercise || modal?.kind !== 'set') {
      return;
    }
    const set = toRequest(exercise.trackingType, input);
    if (modal.editing) {
      actions.updateSet.mutate({ exerciseId: exercise.id, setId: modal.editing.id, set }, { onSuccess: closeModal });
    } else {
      // новый подход: окно закрываем сразу, подход уже в списке («сохраняется…»), запрос и повторы — в фоне
      actions.addSet.mutate({ exerciseId: exercise.id, set, clientId: newClientId() });
      closeModal();
    }
  };

  const next = exercise ? exercise.sets.length + 1 : 1;
  const previousSame = exercise?.previousSets[next - 1];

  // ПК (от 900px): слева текущее упражнение и подходы, справа список упражнений и действия.
  // Телефон: упражнения — полоса чипов сверху, «Записать подход» закреплена у большого пальца.
  const desktop = useMediaQuery(DESKTOP);

  const exerciseList = (
    <section className={styles.group} aria-labelledby="workout-exercises">
      <SectionLabel id="workout-exercises">Упражнения</SectionLabel>
      <ListRowGroup>
        {workout.exercises.map((e, index) => (
          <ListRow
            key={e.id}
            title={`${index + 1}. ${e.exerciseName}`}
            text={setsCountText(e.sets.length + pending.filter((p) => p.workoutExerciseId === e.id).length)}
            selected={e.id === exercise?.id}
            onClick={() => setSelectedId(e.id)}
          />
        ))}
        <ListRow mark="+" title="Добавить упражнение" onClick={() => setModal({ kind: 'addExercise' })} />
      </ListRowGroup>
    </section>
  );

  const links = (
    <div className={styles.links}>
      {exercise && exercise.sets.length === 0 && (
        <button
          type="button"
          className={styles.link}
          onClick={() => actions.removeExercise.mutate(exercise.id)}
          disabled={actions.removeExercise.isPending}
        >
          Убрать упражнение
        </button>
      )}
      {!workout.templateId && workout.exercises.length > 0 && (
        <button type="button" className={styles.link} onClick={() => setModal({ kind: 'saveTemplate' })}>
          Сохранить как комплекс
        </button>
      )}
      <button type="button" className={styles.linkDanger} onClick={() => setModal({ kind: 'cancel' })}>
        Отменить тренировку
      </button>
    </div>
  );

  return (
    <div className={styles.wide}>
      {workout.startedAt ? (
        <PageHeader
          eyebrow="Тренировка идёт"
          title={<span className={styles.timerTitle}>{formatClock(elapsedSec)}</span>}
          subtitle={workout.name}
          action={
            <Button variant="secondary" onClick={() => setModal({ kind: hasSets ? 'complete' : 'cancel' })}>
              Завершить
            </Button>
          }
        />
      ) : (
        // подготовка: выбираем упражнения заранее, время не идёт
        <PageHeader
          title="Подготовка"
          subtitle="Время пойдёт, когда нажмёте «Начать» или запишете первый подход"
          action={
            <Button onClick={() => actions.begin.mutate()} disabled={actions.begin.isPending}>
              Начать
            </Button>
          }
        />
      )}

      <div className={styles.workoutGrid}>
        <div className={styles.workoutMain}>
          {/* текущее упражнение: картинка, название и "Как выполнять" — вся карточка нажимается */}
          {exercise && <ExerciseCard key={exercise.id} exercise={exercise} />}

          {!desktop && (
            <div className={styles.chips} role="group" aria-label="Упражнения тренировки">
              {workout.exercises.map((e, index) => (
                <Chip key={e.id} selected={e.id === exercise?.id} onClick={() => setSelectedId(e.id)}>
                  {index + 1}. {e.exerciseName}
                </Chip>
              ))}
              <Chip onClick={() => setModal({ kind: 'addExercise' })}>+ упражнение</Chip>
            </div>
          )}

          {!exercise && (
            <Card>
              <p className={styles.muted}>В тренировке пока нет упражнений.</p>
              <Button onClick={() => setModal({ kind: 'addExercise' })}>Добавить упражнение</Button>
            </Card>
          )}

          {exercise && (
            <>
              {exercise.sets.length > 0 && (
                <ListRowGroup label={`Подходы: ${exercise.exerciseName}`}>
                  {exercise.sets.map((set) => {
                    const waiting = pendingById.get(set.id);
                    return (
                      <ListRow
                        key={set.id}
                        title={setLabel(exercise.trackingType, set.number, exercise.sets.length)}
                        text={pendingText(waiting, set.warmup)}
                        value={<SetValue trackingType={exercise.trackingType} set={set} assisted={exercise.assisted} />}
                        // сохраняется — менять нельзя (у подхода ещё нет id на сервере); не сохранился — повторить
                        onClick={
                          waiting
                            ? waiting.status === 'failed'
                              ? () => actions.retrySet(waiting)
                              : undefined
                            : () => setModal({ kind: 'set', editing: set })
                        }
                      />
                    );
                  })}
                </ListRowGroup>
              )}

              {/*
                На телефоне — закреплена внизу, над панелью вкладок: главное действие экрана всегда под большим
                пальцем и не уезжает вниз, когда подходов много. Подсказка "прошлый раз" — над кнопкой.
              */}
              <div className={styles.dock} data-dock>
                {previousSame && (
                  <p className={styles.dockHint}>
                    {exercise.trackingType === 'DISTANCE' ? 'Прошлый раз' : 'Прошлый раз на этом подходе'}:{' '}
                    {describeSet(exercise.trackingType, previousSame, exercise.assisted)}
                  </p>
                )}
                <Button fullWidth onClick={() => setModal({ kind: 'set', editing: null })}>
                  {addSetLabel(exercise.trackingType, next)}
                </Button>
              </div>
            </>
          )}

          {!desktop && links}
        </div>

        {desktop && (
          <aside className={styles.workoutSide}>
            {exerciseList}
            {links}
          </aside>
        )}
      </div>

      {modal?.kind === 'set' && exercise && (
        <SetDialog
          exercise={exercise}
          editing={modal.editing}
          saving={actions.updateSet.isPending || actions.deleteSet.isPending}
          error={actions.updateSet.error?.message ?? actions.deleteSet.error?.message ?? null}
          onSave={saveSet}
          onDelete={() =>
            modal.editing &&
            actions.deleteSet.mutate({ exerciseId: exercise.id, setId: modal.editing.id }, { onSuccess: closeModal })
          }
          onClose={closeModal}
        />
      )}

      {modal?.kind === 'addExercise' && (
        <AddExerciseDialog
          existingIds={workout.exercises.map((e) => e.exerciseId)}
          adding={actions.addExercise.isPending}
          onAdd={(exerciseId) =>
            actions.addExercise.mutate(exerciseId, {
              onSuccess: (updated) => {
                setSelectedId(updated.exercises.at(-1)?.id ?? null); // сразу переключаемся на добавленное
                closeModal();
              },
            })
          }
          onClose={closeModal}
        />
      )}

      {modal?.kind === 'saveTemplate' && (
        <SaveTemplateDialog exerciseIds={workout.exercises.map((e) => e.exerciseId)} onClose={closeModal} />
      )}

      {modal?.kind === 'complete' && (
        <ConfirmDialog
          title="Завершить тренировку?"
          text={completeText(workout, saving, failed)}
          confirmLabel={saving > 0 ? 'Сохраняем подходы…' : 'Завершить'}
          // пока подходы отправляются — ждём: иначе тренировка завершилась бы без них
          busy={actions.complete.isPending || saving > 0}
          onConfirm={() => actions.complete.mutate()}
          onClose={closeModal}
        />
      )}

      {modal?.kind === 'cancel' && (
        <ConfirmDialog
          title="Отменить тренировку?"
          text={hasSets ? 'Все записанные подходы удалятся.' : 'В тренировке нет подходов — её можно только отменить.'}
          confirmLabel="Отменить тренировку"
          busy={actions.cancel.isPending}
          onConfirm={() => actions.cancel.mutate()}
          onClose={closeModal}
        />
      )}
    </div>
  );
}

/** Бег без подходов: одна запись — "Пробежка"; несколько — это интервалы, "Отрезок 1, 2…". */
function setLabel(trackingType: TrackingType, number: number, total: number): string {
  if (trackingType === 'DISTANCE') {
    return total === 1 ? 'Пробежка' : `Отрезок ${number}`;
  }
  return `Подход ${number}`;
}

function addSetLabel(trackingType: TrackingType, next: number): string {
  if (trackingType === 'DISTANCE') {
    return next === 1 ? '+ Записать пробежку' : '+ Добавить отрезок (интервалы)';
  }
  return `+ Записать подход ${next}`;
}

/** Значение подхода в строке. У бега темп — отдельной строкой: целиком "5 км · 26:40 · 5:20 /км" на телефон не влезает. */
function SetValue({ trackingType, set, assisted }: { trackingType: TrackingType; set: WorkoutSet; assisted: boolean }) {
  if (trackingType !== 'DISTANCE') {
    return <span className={styles.setValue}>{describeSet(trackingType, set, assisted)}</span>;
  }
  const { main, sub } = runParts(set.distanceM, set.durationSec);
  return (
    <span className={styles.runValue}>
      <span className={styles.setValue}>{main}</span>
      {sub && <span className={styles.muted}>{sub}</span>}
    </span>
  );
}

/** id «неподтверждённого» подхода на экране: у сервера его ещё нет, берём id с телефона */
function pendingSetId(clientId: string): string {
  return `pending-${clientId}`;
}

/** Упражнение для экрана: к сохранённым подходам добавляем неподтверждённые — номера идут дальше по порядку */
function withPendingSets(exercise: WorkoutExercise, pending: PendingSet[]): WorkoutExercise {
  const own = pending.filter((p) => p.workoutExerciseId === exercise.id);
  if (own.length === 0) {
    return exercise;
  }
  const extra: WorkoutSet[] = own.map((p, index) => ({
    ...p.input,
    id: pendingSetId(p.clientId),
    number: exercise.sets.length + index + 1,
  }));
  return { ...exercise, sets: [...exercise.sets, ...extra] };
}

function pendingText(pending: PendingSet | undefined, warmup: boolean): string | undefined {
  if (pending?.status === 'saving') {
    return 'сохраняется…';
  }
  if (pending?.status === 'failed') {
    return 'не сохранён — нажмите, чтобы повторить';
  }
  return warmup ? 'разминка' : undefined;
}

function completeText(workout: Workout, saving: number, failed: number): string {
  const done = workout.exercises.reduce((n, e) => n + e.sets.filter((s) => !s.warmup).length, 0);
  if (saving > 0) {
    return `Ещё сохраняем подходов: ${saving}. Подождите пару секунд — завершим, как только сервер их получит.`;
  }
  const lost =
    failed > 0 ? ` Не сохранились: ${failed} — они не попадут в тренировку (можно вернуться и повторить).` : '';
  return `Подходов: ${done}.${lost} После завершения тренировка попадёт в историю и статистику.`;
}

function setsCountText(count: number): string {
  return count > 0 ? setsLabel(count) : 'ещё нет подходов';
}
