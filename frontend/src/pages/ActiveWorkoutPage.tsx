import { useState } from 'react';
import type { SetInput, Workout, WorkoutSet } from '../api/workouts';
import { Button, ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Chip } from '../components/ui/Chip';
import { AddExerciseDialog } from '../components/workout/AddExerciseDialog';
import { ConfirmDialog } from '../components/workout/ConfirmDialog';
import { SetDialog } from '../components/workout/SetDialog';
import { useNow } from '../hooks/useNow';
import { useCurrentWorkout, useWorkoutActions } from '../hooks/useWorkout';
import { describeSet, formatClock, toRequest } from '../lib/workout';
import styles from './WorkoutPages.module.css';

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
      <div className={styles.page}>
        <h1 className={styles.title}>Тренировка</h1>
        <p className={styles.muted}>Сейчас тренировка не идёт.</p>
        <ButtonLink to="/workout">Начать тренировку</ButtonLink>
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
  | null;

function ActiveWorkout({ workout }: { workout: Workout }) {
  const actions = useWorkoutActions(workout);
  const now = useNow();
  const [selectedId, setSelectedId] = useState<string | null>(workout.exercises[0]?.id ?? null);
  const [modal, setModal] = useState<Modal>(null);

  const exercise = workout.exercises.find((e) => e.id === selectedId) ?? workout.exercises[0] ?? null;
  const elapsedSec = workout.startedAt ? (now - new Date(workout.startedAt).getTime()) / 1000 : 0;
  const hasSets = workout.exercises.some((e) => e.sets.length > 0);

  const setMutation = modal?.kind === 'set' && modal.editing ? actions.updateSet : actions.addSet;
  const closeModal = () => {
    setModal(null);
    actions.addSet.reset();
    actions.updateSet.reset();
    actions.deleteSet.reset();
  };

  const saveSet = (input: SetInput) => {
    if (!exercise || modal?.kind !== 'set') {
      return;
    }
    const set = toRequest(exercise.trackingType, input);
    const options = { onSuccess: closeModal };
    if (modal.editing) {
      actions.updateSet.mutate({ exerciseId: exercise.id, setId: modal.editing.id, set }, options);
    } else {
      actions.addSet.mutate({ exerciseId: exercise.id, set }, options);
    }
  };

  const next = exercise ? exercise.sets.length + 1 : 1;
  const previousSame = exercise?.previousSets[next - 1];

  return (
    <div className={styles.page}>
      {workout.startedAt ? (
        <div className={styles.topBar}>
          <p className={styles.timerBlock}>
            <span className={styles.muted}>Тренировка идёт</span>
            <span className={styles.timer}>{formatClock(elapsedSec)}</span>
          </p>
          <Button variant="secondary" onClick={() => setModal({ kind: hasSets ? 'complete' : 'cancel' })}>
            Завершить
          </Button>
        </div>
      ) : (
        // подготовка: выбираем упражнения заранее, время не идёт
        <div className={styles.topBar}>
          <p className={styles.timerBlock}>
            <span className={styles.timer}>Подготовка</span>
            <span className={styles.muted}>Время пойдёт, когда нажмёте «Начать» или запишете первый подход</span>
          </p>
          <Button onClick={() => actions.begin.mutate()} disabled={actions.begin.isPending}>
            Начать
          </Button>
        </div>
      )}

      <h1 className={styles.title}>{exercise?.exerciseName ?? workout.name}</h1>

      <div className={styles.chips} role="group" aria-label="Упражнения тренировки">
        {workout.exercises.map((e, index) => (
          <Chip key={e.id} selected={e.id === exercise?.id} onClick={() => setSelectedId(e.id)}>
            {index + 1}. {e.exerciseName}
          </Chip>
        ))}
        <Chip onClick={() => setModal({ kind: 'addExercise' })}>+ упражнение</Chip>
      </div>

      {!exercise && (
        <Card>
          <p className={styles.muted}>В тренировке пока нет упражнений.</p>
          <Button onClick={() => setModal({ kind: 'addExercise' })}>Добавить упражнение</Button>
        </Card>
      )}

      {exercise && (
        <>
          {exercise.sets.length > 0 && (
            <ul className={styles.sets}>
              {exercise.sets.map((set) => (
                <li key={set.id}>
                  <button
                    type="button"
                    className={styles.setRow}
                    onClick={() => setModal({ kind: 'set', editing: set })}
                    aria-label={`Подход ${set.number}: ${describeSet(exercise.trackingType, set)}. Изменить`}
                  >
                    <span>
                      Подход {set.number}
                      {set.warmup && <span className={styles.muted}> · разминка</span>}
                    </span>
                    <span className={styles.setValue}>{describeSet(exercise.trackingType, set)}</span>
                    <svg
                      className={styles.check}
                      aria-hidden="true"
                      width="22"
                      height="22"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2.5"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M5 12l5 5L20 7" />
                    </svg>
                  </button>
                </li>
              ))}
            </ul>
          )}

          <Button fullWidth onClick={() => setModal({ kind: 'set', editing: null })}>
            + Записать подход {next}
          </Button>
          {previousSame && (
            <p className={styles.hint}>
              Прошлый раз на этом подходе: {describeSet(exercise.trackingType, previousSame)}
            </p>
          )}
        </>
      )}

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
        <button type="button" className={styles.linkDanger} onClick={() => setModal({ kind: 'cancel' })}>
          Отменить тренировку
        </button>
      </div>

      {modal?.kind === 'set' && exercise && (
        <SetDialog
          exercise={exercise}
          editing={modal.editing}
          saving={setMutation.isPending || actions.deleteSet.isPending}
          error={setMutation.error?.message ?? actions.deleteSet.error?.message ?? null}
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

      {modal?.kind === 'complete' && (
        <ConfirmDialog
          title="Завершить тренировку?"
          text={`Подходов: ${workout.exercises.reduce((n, e) => n + e.sets.filter((s) => !s.warmup).length, 0)}. После завершения тренировка попадёт в историю и статистику.`}
          confirmLabel="Завершить"
          busy={actions.complete.isPending}
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
