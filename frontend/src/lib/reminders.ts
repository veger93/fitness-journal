import type { DayOfWeek, WeeklyPlan } from '../api/workouts';
import { DAYS } from './plan';

/** Дни, на которые в плане стоит комплекс, — в порядке недели */
export function planDays(plan: WeeklyPlan | undefined): DayOfWeek[] {
  return (plan?.days ?? []).filter((d) => d.template !== null).map((d) => d.dayOfWeek);
}

/** Выбрать / снять день. Порядок — как в неделе, чтобы сравнение "совпадает с планом" не зависело от кликов. */
export function toggleDay(days: DayOfWeek[], day: DayOfWeek): DayOfWeek[] {
  const next = new Set(days);
  if (next.has(day)) {
    next.delete(day);
  } else {
    next.add(day);
  }
  return DAYS.map((d) => d.day).filter((d) => next.has(d));
}

export function sameDays(a: DayOfWeek[], b: DayOfWeek[]): boolean {
  return a.length === b.length && a.every((d) => b.includes(d));
}

/** Сервер отдаёт время как "19:00:00", поле <input type="time"> ждёт "19:00" */
export function toTimeInput(time: string | null): string {
  return time ? time.slice(0, 5) : '19:00';
}

/** Часовой пояс устройства: "Europe/Moscow". Напоминание придёт в 19:00 по местному времени, а не по UTC. */
export function deviceTimeZone(): string {
  return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
}
