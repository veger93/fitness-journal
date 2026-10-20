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
