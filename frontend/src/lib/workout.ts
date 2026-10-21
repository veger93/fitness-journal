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

/** Пробежка по умолчанию: 5 км за 30 минут — дальше человек поправит под себя. */
const DEFAULT_RUN: SetInput = { ...DEFAULT_SET, distanceM: 5000, durationSec: 30 * 60 };

/**
 * Что подставить в форму нового подхода (чтобы не набирать каждый раз):
 * 1) последний подход этого упражнения сегодня; 2) тот же по номеру подход в прошлый раз; 3) значения по умолчанию.
 */
export function suggestNextSet(
  sets: WorkoutSet[],
  previousSets: WorkoutSet[],
  trackingType: TrackingType = 'WEIGHT_REPS',
): SetInput {
  const defaults = trackingType === 'DISTANCE' ? DEFAULT_RUN : DEFAULT_SET;
  const source = sets.at(-1) ?? previousSets[sets.length] ?? previousSets.at(-1);
  if (!source) {
    return { ...defaults };
  }
  return {
    weightKg: source.weightKg ?? defaults.weightKg,
    reps: source.reps ?? defaults.reps,
    durationSec: source.durationSec ?? defaults.durationSec,
    distanceM: source.distanceM ?? defaults.distanceM,
    rpe: trackingType === 'DISTANCE' ? source.rpe : null,
    warmup: false,
  };
}

/** В запрос отправляем только поля, нужные этому виду упражнения. */
export function toRequest(trackingType: TrackingType, set: SetInput): SetInput {
  return {
    weightKg: trackingType === 'WEIGHT_REPS' ? set.weightKg : null,
    reps: trackingType === 'WEIGHT_REPS' ? set.reps : null,
    // у бега время тоже нужно — без него нет темпа
    durationSec: trackingType === 'TIME' || trackingType === 'DISTANCE' ? set.durationSec : null,
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
      return describeRun(set.distanceM, set.durationSec);
  }
}

/** "5 км · 26:40 · 5:20 /км" — дистанция, время и темп (если время известно). */
export function describeRun(distanceM: number | null, durationSec: number | null): string {
  const distance = formatDistance(distanceM ?? 0);
  if (!durationSec || !distanceM) {
    return distance;
  }
  return `${distance} · ${formatClock(durationSec)} · ${formatPace(durationSec, distanceM)}`;
}

/** Для строки отрезка: крупно "5 км · 26:40", мелко под ним темп. */
export function runParts(distanceM: number | null, durationSec: number | null): { main: string; pace: string | null } {
  const distance = formatDistance(distanceM ?? 0);
  if (!durationSec || !distanceM) {
    return { main: distance, pace: null };
  }
  return { main: `${distance} · ${formatClock(durationSec)}`, pace: formatPace(durationSec, distanceM) };
}

/** 5000 -> "5 км", 800 -> "800 м" */
export function formatDistance(meters: number): string {
  return meters >= 1000 ? `${formatNumber(meters / 1000)} км` : `${meters} м`;
}

/** Темп — минуты на километр: 1600 с на 5000 м -> "5:20 /км". Главная цифра бегуна. */
export function formatPace(durationSec: number, distanceM: number): string {
  return `${formatClock((durationSec / distanceM) * 1000)} /км`;
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
