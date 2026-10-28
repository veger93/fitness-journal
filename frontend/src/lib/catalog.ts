import type { ExerciseSummary } from '../api/analytics';
import type { BodyPart, CatalogExercise, Equipment } from '../api/workouts';
import { plural } from './format';

export const EQUIPMENT_NAMES: Record<Equipment, string> = {
  BARBELL: 'Штанга',
  DUMBBELL: 'Гантели',
  MACHINE: 'Тренажёр',
  BODYWEIGHT: 'Свой вес',
};

/** Порядок плиток и картинка на плитке — узнаваемое упражнение этой части тела */
export const BODY_PART_TILES: { bodyPart: BodyPart; imageKey: string }[] = [
  { bodyPart: 'CHEST', imageKey: 'bench-press' },
  { bodyPart: 'BACK', imageKey: 'pull-ups' },
  { bodyPart: 'LEGS', imageKey: 'squat' },
  { bodyPart: 'SHOULDERS', imageKey: 'barbell-shoulder-press' },
  { bodyPart: 'ARMS', imageKey: 'barbell-curl' },
  { bodyPart: 'CORE', imageKey: 'crunches' },
];

/** Сколько упражнений в каждой части тела — для подписи на плитке */
export function countByBodyPart(exercises: CatalogExercise[]): Record<BodyPart, number> {
  const counts: Record<BodyPart, number> = { CHEST: 0, BACK: 0, LEGS: 0, SHOULDERS: 0, ARMS: 0, CORE: 0 };
  for (const exercise of exercises) {
    counts[exercise.bodyPart] += 1;
  }
  return counts;
}

export interface CatalogFilter {
  search: string;
  bodyPart: BodyPart | null;
  equipment: Equipment | null;
  /** только свои упражнения ("Мои упражнения") */
  customOnly?: boolean;
}

/**
 * Фильтр каталога на клиенте. Каталог маленький (десятки упражнений), поэтому грузим его один раз целиком
 * и фильтруем в браузере: переключение плиток и чипов мгновенное, без запроса на каждое нажатие.
 * Поиск — по всему каталогу, без учёта категории: если человек печатает, он знает, что ищет.
 */
export function filterCatalog(exercises: CatalogExercise[], filter: CatalogFilter): CatalogExercise[] {
  const query = filter.search.trim().toLowerCase();
  if (query) {
    return exercises.filter((e) => e.name.toLowerCase().includes(query));
  }
  return exercises.filter(
    (e) =>
      (!filter.customOnly || e.custom) &&
      (filter.bodyPart === null || e.bodyPart === filter.bodyPart) &&
      (filter.equipment === null || e.equipment === filter.equipment),
  );
}

/** Снаряды, которые реально есть в списке, — показываем только их чипы (в "Прессе" нет штанги — нет и чипа) */
export function equipmentIn(exercises: CatalogExercise[]): Equipment[] {
  const present = new Set(exercises.map((e) => e.equipment));
  return (Object.keys(EQUIPMENT_NAMES) as Equipment[]).filter((eq) => present.has(eq));
}

/**
 * Недавние упражнения: из сводки аналитики (там у каждого упражнения дата последней тренировки),
 * свежие первыми. Берём только те, что есть в каталоге (своё упражнение могли удалить).
 */
export function recentExercises(
  summaries: ExerciseSummary[],
  catalog: CatalogExercise[],
  limit = 5,
): CatalogExercise[] {
  const byId = new Map(catalog.map((e) => [e.id, e]));
  return [...summaries]
    .sort((a, b) => b.lastPerformedAt.localeCompare(a.lastPerformedAt))
    .map((s) => byId.get(s.exerciseId))
    .filter((e): e is CatalogExercise => e !== undefined)
    .slice(0, limit);
}

/** "8 упражнений", "1 упражнение", "3 упражнения" */
export function exercisesCount(n: number): string {
  return plural(n, 'упражнение', 'упражнения', 'упражнений');
}
