import { describeRun, formatClock } from './workout';

/** Форматирование чисел и дат для экрана. Всё в одном месте — одинаково на всех страницах. */

const numberFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 });
const shortDate = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short' });
const monthName = new Intl.DateTimeFormat('ru-RU', { month: 'long', timeZone: 'UTC' });

/** 102.5 -> "102,5 кг" */
export function formatKg(kg: number): string {
  return `${numberFormat.format(kg)} кг`;
}

/** Тоннаж: до тонны — в кг, дальше — в тоннах: 14230 -> "14,2 т" */
export function formatTonnage(kg: number): string {
  return kg >= 1000 ? `${numberFormat.format(kg / 1000)} т` : formatKg(kg);
}

/** 8 -> "+8%", -2.5 -> "−2,5%" (настоящий минус, а не дефис) */
export function formatChange(percent: number): string {
  const sign = percent > 0 ? '+' : percent < 0 ? '−' : '';
  return `${sign}${numberFormat.format(Math.abs(percent))}%`;
}

/** "80 кг × 5"; без веса — только повторы */
export function formatSet(weightKg: number | null, reps: number | null): string | null {
  if (weightKg == null) {
    return reps == null ? null : `${reps} повт.`;
  }
  return reps == null ? formatKg(weightKg) : `${numberFormat.format(weightKg)} кг × ${reps}`;
}

/** ISO-дата -> "18 окт." */
export function formatShortDate(iso: string | number): string {
  return shortDate.format(new Date(iso));
}

/** "2026-10" -> "октябрь" */
export function formatMonth(month: string): string {
  return monthName.format(new Date(`${month}-01T00:00:00Z`));
}

/** Последний результат упражнения одной строкой — по его виду: "80 кг × 5", "12 повт.", "1:30", "5 км · 26:40 · 5:20 /км". */
export function formatExerciseResult(e: {
  trackingType: 'WEIGHT_REPS' | 'TIME' | 'DISTANCE';
  lastWeightKg: number | null;
  lastReps: number | null;
  lastDistanceM: number | null;
  lastDurationSec: number | null;
}): string | null {
  switch (e.trackingType) {
    case 'DISTANCE':
      return e.lastDistanceM ? describeRun(e.lastDistanceM, e.lastDurationSec) : null;
    case 'TIME':
      return e.lastDurationSec ? formatClock(e.lastDurationSec) : null;
    default:
      return formatSet(e.lastWeightKg, e.lastReps);
  }
}

/** Русское множественное число: plural(1, 'подход', 'подхода', 'подходов') → "1 подход", 3 → "3 подхода", 11 → "11 подходов" */
export function plural(n: number, one: string, few: string, many: string): string {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) {
    return `${n} ${one}`;
  }
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
    return `${n} ${few}`;
  }
  return `${n} ${many}`;
}
