import type { PersonalRecord } from '../api/analytics';
import type { Workout } from '../api/workouts';

export interface MonthGroup {
  /** "2026-10" — ключ для React */
  key: string;
  /** "Октябрь 2026" */
  title: string;
  workouts: Workout[];
}

const monthTitle = new Intl.DateTimeFormat('ru-RU', { month: 'long', year: 'numeric' });

function workoutDate(workout: Workout): Date {
  return new Date(workout.startedAt ?? workout.completedAt ?? 0);
}

/**
 * Длинную историю делим на месяцы: заголовок "Октябрь 2026" — и сразу понятно, где ты в ленте.
 * Тренировки уже отсортированы сервером (новые сверху), поэтому группы идут подряд — достаточно одного прохода.
 * Месяц — по часам пользователя: тренировка 31-го в 23:00 остаётся в своём месяце.
 */
export function groupByMonth(workouts: Workout[]): MonthGroup[] {
  const groups: MonthGroup[] = [];
  for (const workout of workouts) {
    const date = workoutDate(workout);
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    const last = groups.at(-1);
    if (last?.key === key) {
      last.workouts.push(workout);
    } else {
      const title = monthTitle.format(date).replace(' г.', '');
      groups.push({ key, title: title.charAt(0).toUpperCase() + title.slice(1), workouts: [workout] });
    }
  }
  return groups;
}

/** workoutId -> упражнения, в которых в этой тренировке поставлен рекорд */
export function recordsByWorkout(records: PersonalRecord[]): Map<string, Set<string>> {
  const result = new Map<string, Set<string>>();
  for (const record of records) {
    const set = result.get(record.workoutId) ?? new Set<string>();
    set.add(record.exerciseId);
    result.set(record.workoutId, set);
  }
  return result;
}
