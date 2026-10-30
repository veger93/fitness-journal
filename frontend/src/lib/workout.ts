import type { Deload, Equipment, SetInput, TrackingType, Workout, WorkoutSet } from '../api/workouts';

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
 * Стартовый вес, когда упражнение делается впервые — по снаряжению:
 * пустой гриф 20 кг, лёгкие гантели, тренажёр с небольшого веса, свой вес — без отягощения.
 * Дальше человек поправит под себя, а в следующий раз подставится уже его прошлый результат.
 */
const FIRST_TIME_WEIGHT: Record<Equipment, number> = {
  BARBELL: 20,
  DUMBBELL: 6,
  MACHINE: 20,
  BODYWEIGHT: 0,
};

/**
 * Что подставить в форму нового подхода (чтобы не набирать каждый раз):
 * 0) идёт разгрузка — её вес и повторы, а не "как в прошлый раз" (иначе подсказка тянула бы обратно к тяжёлому);
 * 1) последний подход этого упражнения сегодня — продолжаем с того веса, на котором остановились;
 * 2) первый подход сегодня — самый тяжёлый рабочий подход прошлой тренировки: человек помнит
 *    "в прошлый раз жал 40", а не "начинал с 20" (разминку не считаем);
 * 3) упражнение впервые — стартовые значения по снаряжению.
 */
export function suggestNextSet(
  sets: WorkoutSet[],
  previousSets: WorkoutSet[],
  trackingType: TrackingType = 'WEIGHT_REPS',
  equipment: Equipment = 'BARBELL',
  deload: Deload | null = null,
): SetInput {
  const defaults =
    trackingType === 'DISTANCE' ? DEFAULT_RUN : { ...DEFAULT_SET, weightKg: FIRST_TIME_WEIGHT[equipment] };
  if (deload && trackingType === 'WEIGHT_REPS') {
    return {
      ...defaults,
      weightKg: deload.workingWeightKg ?? sets.at(-1)?.weightKg ?? defaults.weightKg,
      reps: deload.reps,
    };
  }
  const source = sets.at(-1) ?? heaviestSet(previousSets);
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

/**
 * Самый тяжёлый рабочий подход (разминочные не считаем, если есть рабочие).
 * При равном весе — последний из них: его повторы ближе к тому, что человек может сейчас.
 * Без веса (планка, бег) — просто последний подход.
 */
export function heaviestSet(sets: WorkoutSet[]): WorkoutSet | undefined {
  const working = sets.some((s) => !s.warmup) ? sets.filter((s) => !s.warmup) : sets;
  return working.reduce<WorkoutSet | undefined>(
    (best, s) => (best === undefined || (s.weightKg ?? 0) >= (best.weightKg ?? 0) ? s : best),
    undefined,
  );
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

/**
 * Бег одной строкой: "5 км · 37:30 · 8 км/ч".
 * Скорость (км/ч) понятна всем и её показывает беговая дорожка; темп (мин/км) — в runParts, вторым планом.
 */
export function describeRun(distanceM: number | null, durationSec: number | null): string {
  const distance = formatDistance(distanceM ?? 0);
  if (!durationSec || !distanceM) {
    return distance;
  }
  return `${distance} · ${formatClock(durationSec)} · ${formatSpeed(durationSec, distanceM)}`;
}

/** Для строки отрезка: крупно "5 км · 37:30", мелко под ним "8 км/ч · 7:30 /км". */
export function runParts(distanceM: number | null, durationSec: number | null): { main: string; sub: string | null } {
  const distance = formatDistance(distanceM ?? 0);
  if (!durationSec || !distanceM) {
    return { main: distance, sub: null };
  }
  return {
    main: `${distance} · ${formatClock(durationSec)}`,
    sub: `${formatSpeed(durationSec, distanceM)} · ${formatPace(durationSec, distanceM)}`,
  };
}

/** Средняя скорость: 5000 м за 37:30 -> "8 км/ч". */
export function formatSpeed(durationSec: number, distanceM: number): string {
  return `${formatNumber(Math.round(speedKmh(durationSec, distanceM) * 10) / 10)} км/ч`;
}

export function speedKmh(durationSec: number, distanceM: number): number {
  return distanceM / 1000 / (durationSec / 3600);
}

/** Беговая дорожка показывает скорость и время — дистанцию считаем сами: км/ч × часы. */
export function distanceFromSpeed(speedKmhValue: number, durationSec: number): number {
  return Math.round((speedKmhValue * durationSec) / 3.6);
}

/** 5000 -> "5 км", 800 -> "800 м" */
export function formatDistance(meters: number): string {
  return meters >= 1000 ? `${formatNumber(meters / 1000)} км` : `${meters} м`;
}

/** Темп — сколько минут уходит на 1 км: 1600 с на 5000 м -> "5:20 /км". Так считают бегуны на улице. */
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

/**
 * Все рабочие подходы упражнения одной строкой — для журнала:
 * "80 кг × 8, 8, 6 · 85 кг × 5" (одинаковый вес подряд не повторяем), "12, 10, 8 повт.", "1:30, 1:15".
 */
export function summarizeSets(trackingType: TrackingType, sets: WorkoutSet[]): string {
  const working = sets.filter((s) => !s.warmup);
  if (working.length === 0) {
    return 'только разминка';
  }
  if (trackingType === 'TIME') {
    return working.map((s) => formatClock(s.durationSec ?? 0)).join(', ');
  }
  if (trackingType === 'DISTANCE') {
    return working.map((s) => describeRun(s.distanceM, s.durationSec)).join(', ');
  }
  if (working.every((s) => !s.weightKg)) {
    return `${working.map((s) => s.reps).join(', ')} повт.`;
  }
  const groups: { weight: number; reps: (number | null)[] }[] = [];
  for (const set of working) {
    const last = groups.at(-1);
    if (last && last.weight === (set.weightKg ?? 0)) {
      last.reps.push(set.reps);
    } else {
      groups.push({ weight: set.weightKg ?? 0, reps: [set.reps] });
    }
  }
  return groups.map((g) => `${formatNumber(g.weight)} кг × ${g.reps.join(', ')}`).join(' · ');
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
