/**
 * Метка "этот вход — демо" в sessionStorage (docs/demo.md). Нужна, чтобы понять, ПОЧЕМУ пропал вход:
 * демо закончилось (сервер больше не продлевает токен) — показать на лендинге "Демо закончилось",
 * а не форму входа, в которую гостю нечего вводить.
 * sessionStorage может быть недоступен (приватный режим, запрет сайта) — тогда просто работаем без метки.
 */
const KEY = 'vegas.demo';

export function markDemoSession(): void {
  try {
    sessionStorage.setItem(KEY, '1');
  } catch {
    // без метки демо работает, просто без сообщения "Демо закончилось"
  }
}

/** Вышли сами ("Выйти из демо", "Зарегистрироваться") — сообщение о конце демо не нужно */
export function clearDemoSession(): void {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // нечего чистить
  }
}

export function hasDemoSession(): boolean {
  try {
    return sessionStorage.getItem(KEY) !== null;
  } catch {
    return false;
  }
}

/** "23 ч", "40 мин" — сколько осталось до удаления демо */
export function demoTimeLeft(expiresAt: string, now: Date = new Date()): string {
  const minutes = Math.max(0, Math.floor((new Date(expiresAt).getTime() - now.getTime()) / 60_000));
  return minutes >= 60 ? `${Math.floor(minutes / 60)} ч` : `${minutes} мин`;
}
