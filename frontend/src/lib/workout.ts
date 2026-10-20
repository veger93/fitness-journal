import type { SetInput, TrackingType, Workout, WorkoutSet } from '../api/workouts';

/**
 * Логика экрана тренировки без React: что подставить в новый подход, как показать подход,
 * как обновить тренировку в кэше. Всё — чистые функции, поэтому покрыто тестами.
 */

const DEFAULT_SET: SetInput = {
  weightKg: 20,
  reps: 10,
  durationSec: 60,
  distanceM: 1000,
  rpe: null,
  warmup: false,
};

/**
 * Что подставить в форму нового подхода (чтобы не набирать каждый раз):
 * 1) последний подход этого упражнения сегодня; 2) тот же по номеру подход в прошлый раз; 3) значения по умолчанию.
 */
export function suggestNextSet(sets: WorkoutSet[], previousSets: WorkoutSet[]): SetInput {
  const source = sets.at(-1) ?? previousSets[sets.length] ?? previousSets.at(-1);
  if (!source) {
    return { ...DEFAULT_SET };
  }
  return {
    weightKg: source.weightKg ?? DEFAULT_SET.weightKg,
    reps: source.reps ?? DEFAULT_SET.reps,
    durationSec: source.durationSec ?? DEFAULT_SET.durationSec,
    distanceM: source.distanceM ?? DEFAULT_SET.distanceM,
    rpe: null,
    warmup: false,
  };
}

/** В запрос отправляем только поля, нужные этому виду упражнения. */
export function toRequest(trackingType: TrackingType, set: SetInput): SetInput {
  return {
    weightKg: trackingType === 'WEIGHT_REPS' ? set.weightKg : null,
    reps: trackingType === 'WEIGHT_REPS' ? set.reps : null,
    durationSec: trackingType === 'TIME' ? set.durationSec : null,
    distanceM: trackingType === 'DISTANCE' ? set.distanceM : null,
    rpe: set.rpe,
    warmup: set.warmup,
  };
}

/** "80 кг × 8", "1:30", "1,2 км" — подход для списка. */
export function describeSet(
  trackingType: TrackingType,
  set: Pick<SetInput, 'weightKg' | 'reps' | 'durationSec' | 'distanceM'>,
): string {
  switch (trackingType) {
    case 'WEIGHT_REPS':
      return set.weightKg ? `${formatNumber(set.weightKg)} кг × ${set.reps}` : `${set.reps} повт.`;
    case 'TIME':
      return formatClock(set.durationSec ?? 0);
    case 'DISTANCE':
      return (set.distanceM ?? 0) >= 1000 ? `${formatNumber((set.distanceM ?? 0) / 1000)} км` : `${set.distanceM} м`;
  }
}

/** Секунды -> "24:11" или "1:02:05" (часы — только если они есть) */
export function formatClock(totalSec: number): string {
  const sec = Math.max(0, Math.floor(totalSec));
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  const mm = h > 0 ? String(m).padStart(2, '0') : String(m);
  return `${h > 0 ? `${h}:` : ''}${mm}:${String(s).padStart(2, '0')}`;
}

/** Секунды -> "42 мин", "1 ч 10 мин" */
export function formatDuration(totalSec: number): string {
  const minutes = Math.round(totalSec / 60);
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) {
    return `${m} мин`;
  }
  return m === 0 ? `${h} ч` : `${h} ч ${m} мин`;
}

// ---------- обновление тренировки в кэше (иммутабельно: новые объекты вместо изменения старых) ----------

export function withSetAdded(workout: Workout, workoutExerciseId: string, set: WorkoutSet): Workout {
  return mapExercise(workout, workoutExerciseId, (sets) => [...sets, set]);
}

export function withSetUpdated(workout: Workout, workoutExerciseId: string, set: WorkoutSet): Workout {
  return mapExercise(workout, workoutExerciseId, (sets) => sets.map((s) => (s.id === set.id ? set : s)));
}

/** После удаления сервер перенумеровывает подходы (1, 2, 3...) — делаем так же, чтобы экран совпадал */
export function withSetDeleted(workout: Workout, workoutExerciseId: string, setId: string): Workout {
  return mapExercise(workout, workoutExerciseId, (sets) =>
    sets.filter((s) => s.id !== setId).map((s, index) => ({ ...s, number: index + 1 })),
  );
}

function mapExercise(
  workout: Workout,
  workoutExerciseId: string,
  change: (sets: WorkoutSet[]) => WorkoutSet[],
): Workout {
  return {
    ...workout,
    exercises: workout.exercises.map((e) => (e.id === workoutExerciseId ? { ...e, sets: change(e.sets) } : e)),
  };
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(value);
}
