import type { WorkoutTemplate } from '../api/workouts';

/** Цвета меток комплексов — читаются и на светлой, и на тёмной карточке. Ключи — как в палитре бэкенда */
export const TEMPLATE_COLORS: Record<string, string> = {
  violet: '#7c3aed',
  green: '#16a34a',
  blue: '#2563eb',
  orange: '#ea580c',
};
const PALETTE = Object.values(TEMPLATE_COLORS);

/** Свой цвет у комплекса (у готовых он есть); нет — стабильный цвет по id, чтобы метка не "прыгала" */
export function templateColor(template: Pick<WorkoutTemplate, 'id' | 'color'>): string {
  if (template.color && TEMPLATE_COLORS[template.color]) {
    return TEMPLATE_COLORS[template.color];
  }
  let hash = 0;
  for (const char of template.id) {
    hash = (hash * 31 + char.charCodeAt(0)) | 0;
  }
  return PALETTE[Math.abs(hash) % PALETTE.length];
}

/**
 * Чем тренировка отличается от своего комплекса: какие упражнения добавили и какие убрали.
 * Порядок не считаем изменением — переставить упражнения в тренировке нормально, комплекс от этого не «устарел».
 */
export function templateChanges(templateIds: string[], workoutIds: string[]): { added: string[]; removed: string[] } {
  const before = new Set(templateIds);
  const after = new Set(workoutIds);
  return {
    added: workoutIds.filter((id) => !before.has(id)),
    removed: templateIds.filter((id) => !after.has(id)),
  };
}

/** Кому из комплексов можно предложить «обновить»: только свой и не день программы (его меняют в программе) */
export function canUpdateTemplate(template: Pick<WorkoutTemplate, 'custom' | 'program'> | undefined): boolean {
  return Boolean(template && template.custom && !template.program);
}

/** Переставить элемент на одну позицию вверх (-1) или вниз (+1); за край — без изменений */
export function move<T>(list: T[], index: number, delta: -1 | 1): T[] {
  const target = index + delta;
  if (target < 0 || target >= list.length) {
    return list;
  }
  const copy = [...list];
  [copy[index], copy[target]] = [copy[target], copy[index]];
  return copy;
}
