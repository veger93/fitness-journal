/**
 * Месяцы в формате "2026-10" — так их принимает и отдаёт analytics (YearMonth в Java).
 * Работаем со строками и числами, без Date: у Date есть часовые пояса, и "1 ноября 00:00" легко превращается в 31 октября.
 */

function parse(month: string): { year: number; month: number } {
  const [year, m] = month.split('-').map(Number);
  return { year, month: m };
}

function format(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

/** Текущий месяц по часам пользователя */
export function currentMonth(now: Date = new Date()): string {
  return format(now.getFullYear(), now.getMonth() + 1);
}

/** "2026-01" + (-1) -> "2025-12" */
export function addMonths(month: string, delta: number): string {
  const { year, month: m } = parse(month);
  const index = year * 12 + (m - 1) + delta; // месяцы подряд с нулевого года — так переход через год не нужен отдельно
  return format(Math.floor(index / 12), (index % 12) + 1);
}

const monthNames = new Intl.DateTimeFormat('ru-RU', { month: 'long', timeZone: 'UTC' });

/** "2026-10" -> "октябрь 2026" */
export function monthTitle(month: string): string {
  const { year, month: m } = parse(month);
  return `${monthNames.format(Date.UTC(year, m - 1, 1))} ${year}`;
}

/**
 * Окно "Итоги месяца" показываем первую неделю нового месяца — про прошлый.
 * Возвращает прошлый месяц, если сегодня 1–7 число, иначе null.
 */
export function summaryMonthToShow(now: Date = new Date()): string | null {
  return now.getDate() <= 7 ? addMonths(currentMonth(now), -1) : null;
}
