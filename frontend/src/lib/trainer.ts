import type { ClientSignal } from '../api/trainer';

/** Мелкие расчёты режима тренера — чистые функции, проверены в trainer.test.ts */

/** Буква для кружка-аватара: "Иван Петров" -> "И", пустое имя -> "?" */
export function initial(name: string | null | undefined): string {
  const letter = name?.trim().charAt(0);
  return letter ? letter.toUpperCase() : '?';
}

/** Сколько минут идёт тренировка: для подписи "идёт · 38 мин" */
export function minutesSince(startedAt: string, now: Date = new Date()): number {
  return Math.max(0, Math.floor((now.getTime() - new Date(startedAt).getTime()) / 60_000));
}

/** "38 мин", "1 ч 05 мин" */
export function formatDuration(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} мин`;
  }
  return `${Math.floor(minutes / 60)} ч ${String(minutes % 60).padStart(2, '0')} мин`;
}

/** Подпись незавершённой тренировки клиента: "идёт · 38 мин" или "не начата" (подготовлена заранее) */
export function workoutStatusText(startedAt: string | null, now: Date = new Date()): string {
  return startedAt ? `идёт · ${formatDuration(minutesSince(startedAt, now))}` : 'не начата';
}

/** "3 нед", "5 дн": недели — с двух недель, раньше дни точнее */
export function formatDays(days: number): string {
  return days >= 14 ? `${Math.floor(days / 7)} нед` : `${days} дн`;
}

/**
 * Метка клиента в списке тренера: текст и вид бейджа — как у статусов упражнений на главной
 * (перегрузка — оранжевый, рекорд — контрастный, плато и "давно не был" — нейтральные). null — метки нет.
 */
export function signalBadge(
  signal: ClientSignal | undefined,
): { text: string; variant: 'accent' | 'inverse' | 'neutral' } | null {
  switch (signal?.signal) {
    case 'OVERLOAD':
      return { text: 'перегрузка', variant: 'accent' };
    case 'PLATEAU':
      return { text: `плато · ${formatDays(signal.days ?? 0)}`, variant: 'neutral' };
    case 'AWAY':
      return { text: `${formatDays(signal.days ?? 0)} без тренировок`, variant: 'neutral' };
    case 'RECORD':
      return {
        text: signal.days === 0 ? 'рекорд · сегодня' : signal.days === 1 ? 'рекорд · вчера' : 'рекорд',
        variant: 'inverse',
      };
    default:
      return null;
  }
}
