import type { BodyPart, DayOfWeek, WeeklyPlan, WorkoutTemplate } from '../api/workouts';

export const DAYS: { day: DayOfWeek; name: string; short: string }[] = [
  { day: 'MONDAY', name: 'Понедельник', short: 'Пн' },
  { day: 'TUESDAY', name: 'Вторник', short: 'Вт' },
  { day: 'WEDNESDAY', name: 'Среда', short: 'Ср' },
  { day: 'THURSDAY', name: 'Четверг', short: 'Чт' },
  { day: 'FRIDAY', name: 'Пятница', short: 'Пт' },
  { day: 'SATURDAY', name: 'Суббота', short: 'Сб' },
  { day: 'SUNDAY', name: 'Воскресенье', short: 'Вс' },
];

export const BODY_PART_NAMES: Record<BodyPart, string> = {
  CHEST: 'Грудь',
  BACK: 'Спина',
  LEGS: 'Ноги',
  SHOULDERS: 'Плечи',
  ARMS: 'Руки',
  CORE: 'Пресс',
};

/** Сегодняшний день недели в часовом поясе устройства. JS: 0 — воскресенье, у нас неделя с понедельника. */
export function todayOf(date: Date = new Date()): DayOfWeek {
  return DAYS[(date.getDay() + 6) % 7].day;
}

/** Ближайший тренировочный день после сегодняшнего (по кругу недели), если в плане есть хоть один. */
export function nextPlanned(plan: WeeklyPlan, today: DayOfWeek): { day: DayOfWeek; template: WorkoutTemplate } | null {
  const start = DAYS.findIndex((d) => d.day === today);
  for (let i = 1; i <= 7; i++) {
    const entry = plan.days.find((d) => d.dayOfWeek === DAYS[(start + i) % 7].day);
    if (entry?.template) {
      return { day: entry.dayOfWeek, template: entry.template };
    }
  }
  return null;
}

/**
 * Сколько раз за неделю тренируется каждая группа мышц (группа считается раз в день,
 * даже если в комплексе на неё 3 упражнения).
 */
export function bodyPartFrequency(templates: (WorkoutTemplate | null)[]): Map<BodyPart, number> {
  const counts = new Map<BodyPart, number>();
  for (const template of templates) {
    if (!template) {
      continue;
    }
    for (const part of new Set(template.exercises.map((e) => e.bodyPart))) {
      counts.set(part, (counts.get(part) ?? 0) + 1);
    }
  }
  return counts;
}

/**
 * Подсказки к плану простыми словами. Источники — docs/references.md [14]–[16].
 * Мягко: это советы, а не запреты.
 */
export function planHints(templates: (WorkoutTemplate | null)[]): string[] {
  const workouts = templates.filter(Boolean).length;
  const hints: string[] = [];
  if (workouts === 7) {
    hints.push('Без дня отдыха мышцам негде восстановиться. Обычно хватает 3–5 тренировок в неделю.');
  }
  const once = [...bodyPartFrequency(templates)]
    .filter(([part, count]) => count === 1 && part !== 'CORE')
    .map(([part]) => BODY_PART_NAMES[part].toLowerCase());
  if (once.length > 0 && workouts > 1) {
    hints.push(
      `Раз в неделю: ${once.join(', ')}. Если тренировать группу мышц дважды в неделю, обычно проще набрать нужный объём и расти быстрее.`,
    );
  }
  return hints;
}
