import type { WorkoutExercise } from '../api/workouts';
import { formatKg, plural } from './format';
import { formatRest } from './program';

/**
 * Контроль подходов в тренировке по дню программы (макеты S1, S3, S4): сколько подходов запланировано,
 * сколько сделано, что осталось. Без React — чтобы покрыть тестами.
 *
 * Цель берётся из разгрузки, если она идёт (её схема важнее программы), иначе — из программы.
 * Разминочные подходы не считаем. Упражнение, добавленное сверх программы, без цели — в счёт не идёт.
 */

type Exercise = Pick<WorkoutExercise, 'sets' | 'deload' | 'target'>;

/** Сколько рабочих подходов запланировано; null — цели нет (свободная тренировка или упражнение сверх программы) */
export function plannedSets(e: Exercise): number | null {
  return e.deload?.sets ?? e.target?.sets ?? null;
}

/** Сделано рабочих подходов (разминка не в счёт) */
export function doneSets(e: Exercise): number {
  return e.sets.filter((s) => !s.warmup).length;
}

export function isExerciseDone(e: Exercise): boolean {
  const planned = plannedSets(e);
  return planned !== null && doneSets(e) >= planned;
}

export interface WorkoutPlanProgress {
  done: number;
  total: number;
  exercisesDone: number;
  exercisesTotal: number;
  /** что осталось: упражнение и сколько подходов до плана */
  remaining: { id: string; name: string; done: number; planned: number }[];
}

/** Прогресс по плану дня; null — плана нет (ни у одного упражнения цели) */
export function planProgress(
  exercises: (Exercise & Pick<WorkoutExercise, 'id' | 'exerciseName'>)[],
): WorkoutPlanProgress | null {
  const planned = exercises.filter((e) => plannedSets(e) !== null);
  if (planned.length === 0) {
    return null;
  }
  let done = 0;
  let total = 0;
  const remaining: WorkoutPlanProgress['remaining'] = [];
  for (const e of planned) {
    const plan = plannedSets(e)!;
    const made = doneSets(e);
    total += plan;
    // лишние подходы (4 из 3) не «закрывают» другие упражнения
    done += Math.min(made, plan);
    if (made < plan) {
      remaining.push({ id: e.id, name: e.exerciseName, done: made, planned: plan });
    }
  }
  return { done, total, exercisesDone: planned.length - remaining.length, exercisesTotal: planned.length, remaining };
}

/** «план: 3 × 8–12 · отдых 2 мин»; в разгрузку — её схема */
export function planText(e: Exercise): string | null {
  if (e.deload) {
    return `разгрузка: ${e.deload.sets} × ${e.deload.reps}`;
  }
  const t = e.target;
  if (!t) {
    return null;
  }
  const reps = t.repsMin === t.repsMax ? `${t.repsMin}` : `${t.repsMin}–${t.repsMax}`;
  return `план: ${t.sets} × ${reps} · отдых ${formatRest(t.restSec)}`;
}

/**
 * Подсказка веса (П10). Прибавить — с объяснением по прошлым повторам, чтобы человек понимал «почему»
 * и не воспринимал вес как приказ. В разгрузку подсказки о прибавке нет.
 */
export function weightHint(e: Exercise): { kind: 'increase' | 'keep' | 'new'; text: string } | null {
  const t = e.target;
  if (!t || e.deload) {
    return null;
  }
  const reps = t.lastReps.join(', ');
  if (t.suggestion === 'INCREASE' && t.suggestedWeightKg != null && t.baseWeightKg != null) {
    const step = Math.round((t.suggestedWeightKg - t.baseWeightKg) * 100) / 100;
    return {
      kind: 'increase',
      text: `+${formatKg(step)} — в прошлые 2 раза на ${formatKg(t.baseWeightKg)} было ${reps}. Пора прибавить.`,
    };
  }
  if (t.suggestion === 'KEEP' && t.baseWeightKg != null) {
    return {
      kind: 'keep',
      text: `В прошлый раз на ${formatKg(t.baseWeightKg)}: ${reps}. Цель — ${t.repsMax} во всех подходах, тогда прибавим.`,
    };
  }
  return { kind: 'new', text: NEW_EXERCISE_HINT };
}

/** Упражнение без истории — и в программе, и в свободной тренировке */
export const NEW_EXERCISE_HINT = 'Подберите вес: последние 2–3 повтора должны идти тяжело.';

/** «осталось 2 подхода» */
export function leftText(left: number): string {
  return `осталось ${plural(left, 'подход', 'подхода', 'подходов')}`;
}
