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
